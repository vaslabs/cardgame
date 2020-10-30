package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model._
import cardgame.engine.GameOps._
import cardgame.processor.PlayerEventsReader.{AuthorisationTicket, UserResponse}
import cats.Monoid
import cats.effect.IO
import cats.implicits.catsKernelStdMonoidForMap
import cats.syntax.semigroup._
import cats.derived._
import io.circe.Encoder
import io.circe.syntax._
object GameProcessor {

  implicit val vectorClockLongMonoid: Monoid[Long] = Monoid.instance(0L, Math.max)
  implicit val remoteClockMonoid: Monoid[RemoteClock] = MkMonoid[RemoteClock]

  private final val ATOMIC_RECEIVE_AND_SEND = 2


  def behavior(game: Game, randomizer: IO[Int], localClock: Long, remoteClock: RemoteClock)(implicit authEncoder: Encoder[Authorise]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
        case c: Command =>
          val newRemoteClock = remoteClock |+| c.remoteClock
          val updateLocalClock = localClock + ATOMIC_RECEIVE_AND_SEND

          val (gameAffected, event) = game.action(c.action, randomizer, checkIdempotency)(remoteClock, c.remoteClock)
          val publishableEvent = event match {
            case auth: AuthorisePlayer =>
              val plainText = auth.authorise.asJson.noSpaces
              AuthorisationTicket(auth.playerId, plainText, auth.signature, auth.publicKey)
            case event =>
              UserResponse(ClockedResponse(event, newRemoteClock, updateLocalClock))
          }
          ctx.system.eventStream ! EventStream.Publish(publishableEvent)
          c match {
            case rc: ReplyCommand =>
              rc.replyTo ! Right(ClockedResponse(event, newRemoteClock, updateLocalClock))

            case _ =>
          }

          behavior(gameAffected, randomizer, updateLocalClock, newRemoteClock)
        case Get(playerId, replyTo) =>
          replyTo ! Right(personalise(playerId, game))
          Behaviors.same
      }
  }

  def checkIdempotency(requestingEntity: PlayerId)(oldClock: RemoteClock, newClock: RemoteClock): Boolean = {
    (oldClock.vectorClock.getOrElse(requestingEntity, 0L), newClock.vectorClock.get(requestingEntity)) match {
      case (oldClock, Some(newClock)) =>
        oldClock < newClock
      case _ =>
        false
    }
  }


  private def personalise(playerId: PlayerId, game: Game): Game =
    PlayerEventsReader.personaliseGame(playerId, game)


  sealed trait Protocol

  sealed trait Command extends Protocol {
    def action: Action
    def remoteClock: RemoteClock
  }

  sealed trait ReplyCommand extends Command {
    def replyTo: ActorRef[Either[Unit, ClockedResponse]]
  }

  case class RunCommand(
       replyTo: ActorRef[Either[Unit, ClockedResponse]],
       action: Action,
       remoteClock: RemoteClock
  ) extends ReplyCommand

  case class FireAndForgetCommand(action: Action, remoteClock: RemoteClock) extends Command

  sealed trait Query extends Protocol {
    def replyTo: ActorRef[Either[Unit, Game]]
  }

  case class Get(playerId: PlayerId, replyTo: ActorRef[Either[Unit, Game]]) extends Query

}
