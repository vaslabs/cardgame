package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.engine.GameOps._
import cardgame.model._
import cardgame.processor.PlayerEventsReader.UserResponse
import cats.Monoid
import cats.derived._
import cats.effect.IO
import cats.implicits.catsKernelStdMonoidForMap
import cats.syntax.semigroup._

object GameProcessor {

  implicit val vectorClockLongMonoid: Monoid[Long] = Monoid.instance(0L, Math.max)
  implicit val remoteClockMonoid: Monoid[RemoteClock] = MkMonoid[RemoteClock]

  private final val ATOMIC_RECEIVE_AND_SEND = 2


  def behavior(game: Game, randomizer: IO[Int], localClock: Long, remoteClockCopy: RemoteClock)(validSignature: (Game, ClockedAction) => Boolean): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
        case ac: AdminCommand =>
          val updateLocalClock = localClock + ATOMIC_RECEIVE_AND_SEND

          val (gameAffected, event) = game.action(ac.action, randomizer, _ => (_, _) => true)(remoteClockCopy, remoteClockCopy)
          ctx.system.eventStream ! EventStream.Publish(UserResponse(ClockedResponse(event, remoteClockCopy, updateLocalClock)))
          behavior(gameAffected, randomizer, updateLocalClock, remoteClockCopy)(validSignature)

        case c: Command if validSignature(game, c.action) =>

          val remoteClock = RemoteClock.of(c.action.vectorClock)
          val newRemoteClock = remoteClockCopy |+| remoteClock
          val updateLocalClock = localClock + ATOMIC_RECEIVE_AND_SEND

          val (gameAffected, event) = game.action(c.action.action, randomizer, checkIdempotency)(remoteClockCopy, newRemoteClock)
          ctx.system.eventStream ! EventStream.Publish(UserResponse(ClockedResponse(event, newRemoteClock, updateLocalClock)))
          c match {
            case rc: ReplyCommand =>
              rc.replyTo ! Right(ClockedResponse(event, newRemoteClock, updateLocalClock))

            case _ =>
          }

          behavior(gameAffected, randomizer, updateLocalClock, newRemoteClock)(validSignature)
        case Get(playerId, replyTo) =>
          replyTo ! Right(personalise(playerId, game))
          Behaviors.same
        case _ =>
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
    def action: ClockedAction
  }

  sealed trait ReplyCommand extends Command {
    def replyTo: ActorRef[Either[Unit, ClockedResponse]]
  }

  case class RunCommand(
       replyTo: ActorRef[Either[Unit, ClockedResponse]],
       action: ClockedAction
  ) extends ReplyCommand

  case class FireAndForgetCommand(action: ClockedAction) extends Command

  case class AdminCommand(action: Action) extends Protocol

  sealed trait Query extends Protocol {
    def replyTo: ActorRef[Either[Unit, Game]]
  }

  case class Get(playerId: PlayerId, replyTo: ActorRef[Either[Unit, Game]]) extends Query

}
