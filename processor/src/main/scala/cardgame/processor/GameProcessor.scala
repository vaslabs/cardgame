package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model._
import cardgame.engine.GameOps._
import cats.Monoid
import cats.effect.IO
import cats.implicits.catsKernelStdMonoidForMap
import cats.syntax.semigroup._
import cats.derived._
object GameProcessor {

  implicit val vectorClockLongMonoid: Monoid[Long] = Monoid.instance(0L, Math.max)
  implicit val remoteClockMonoid: Monoid[RemoteClock] = MkMonoid[RemoteClock]

  def behavior(game: Game, randomizer: IO[Int], localClock: Long, remoteClock: RemoteClock): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
        case c: ReplyCommand =>
          val newRemoteClock = remoteClock |+| c.remoteClock
          val updateLocalClock = localClock + 1
          val (gameAffected, event) = game.action(c.action, randomizer)
          ctx.system.eventStream !
            EventStream.Publish(ClockedResponse(event, remoteClock, localClock))
          c.replyTo ! Right(ClockedResponse(event, newRemoteClock, updateLocalClock))
          behavior(gameAffected, randomizer, updateLocalClock, newRemoteClock)
        case c: Command =>
          val newRemoteClock = remoteClock |+| c.remoteClock
          val updateLocalClock = localClock + 1
          val (gameAffected, event) = game.action(c.action, randomizer)
          ctx.system.eventStream !
            EventStream.Publish(ClockedResponse(event, newRemoteClock, updateLocalClock))
          behavior(gameAffected, randomizer, updateLocalClock, newRemoteClock)
        case Get(playerId, replyTo) =>
          replyTo ! Right(personalise(playerId, game))
          Behaviors.same
      }
  }

  private def personalise(playerId: PlayerId, game: Game): Game = {
    game match {
      case g @ StartedGame(players, deck, _, _, _, _) =>
        players.indexWhere(_.id == playerId) match {
          case n if n >= 0 =>
            g.copy(players.updated(n, turnVisible(players(n))), turnVisible(deck, playerId))
          case _ => game
        }
      case other => other
    }
  }

  private def turnVisible(player: PlayingPlayer): PlayingPlayer =
        player.copy(hand = player.hand.map(c => VisibleCard(c.id, c.image)))

  private def turnVisible(deck: Deck, playerId: PlayerId): Deck = {
    deck.borrowed.filter(
      b => b.playerId == playerId
    ).map(b => b.cards.map(c => VisibleCard(c.id, c.image)))
      .map(vc => deck.copy(borrowed = Some(BorrowedCards(playerId, vc))))
      .getOrElse(deck)
  }


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
