package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model._
import cardgame.engine.GameOps._
import cats.effect.IO
object GameProcessor {

  def behavior(game: Game, randomizer: IO[Int]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
        case c: ReplyCommand =>
          val (gameAffected, event) = game.action(c.action, randomizer)
          ctx.system.eventStream ! EventStream.Publish(event)
          c.replyTo ! Right(event)
          behavior(gameAffected, randomizer)
        case c: Command =>
          val (gameAffected, event) = game.action(c.action, randomizer)
          ctx.system.eventStream ! EventStream.Publish(event)
          behavior(gameAffected, randomizer)
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
  }

  sealed trait ReplyCommand extends Command {
    def replyTo: ActorRef[Either[Unit, Event]]
  }

  case class RunCommand(replyTo: ActorRef[Either[Unit, Event]], action: Action) extends ReplyCommand
  case class FireAndForgetCommand(action: Action) extends Command

  sealed trait Query extends Protocol {
    def replyTo: ActorRef[Either[Unit, Game]]
  }

  case class Get(playerId: PlayerId, replyTo: ActorRef[Either[Unit, Game]]) extends Query

}
