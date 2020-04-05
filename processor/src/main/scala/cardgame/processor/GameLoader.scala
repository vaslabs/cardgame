package cardgame.processor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model.{Deck, DeckId, StartGame}
import cardgame.processor.GameProcessor.FireAndForgetCommand

object GameLoader {

  def ephemeralBehaviour(
    deckId: DeckId,
    game: ActorRef[GameProcessor.Protocol],
    replyTo: ActorRef[Either[Unit, Unit]]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      ctx.log.info(s"Loading deck ${deckId}")
      ctx.self ! DeckReady(Deck(List.empty))
      Behaviors.receiveMessage {
        case DeckReady(deck) =>
          game ! FireAndForgetCommand(StartGame(deck))
          replyTo ! Right(())
          Behaviors.stopped
      }
  }

  sealed trait Protocol
  case class DeckReady(deck: Deck) extends Protocol
}
