package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model.{BorrowedCard, CardRecovered, ClockedResponse, GotCard, HiddenCard, MoveCard, PlayerId, ShuffledHand, VisibleCard}

object PlayerEventsReader {

  def behavior(playerId: PlayerId, replyTo: ActorRef[ClockedResponse]): Behavior[ClockedResponse] = Behaviors.setup {
    ctx =>
      ctx.system.eventStream ! EventStream.Subscribe(ctx.self)
      readingEvents(playerId, replyTo)
  }

  private def readingEvents(id: PlayerId, replyTo: ActorRef[ClockedResponse]): Behavior[ClockedResponse] = Behaviors.receiveMessage {
    case msg =>
      replyTo ! personalise(msg, id)
      Behaviors.same
  }

  private def personalise(clockedResponse: ClockedResponse, playerId: PlayerId): ClockedResponse = {
    val event = clockedResponse.event match {
      case gc @ GotCard(player, card) if player == playerId =>
        gc.copy(card = VisibleCard(card.id, card.image))
      case mv @ MoveCard(card: HiddenCard, _, to) if (to == playerId) =>
        mv.copy(card = VisibleCard(card.id, card.image))
      case ShuffledHand(player, hand) if player == playerId =>
        ShuffledHand(playerId, hand.map(c => VisibleCard(c.id, c.image)))
      case bc @ BorrowedCard(card, to) if playerId == to =>
        bc.copy(card = VisibleCard(card.id, card.image))
      case mv @ MoveCard(card: VisibleCard, _, _) =>
        mv.copy(card = HiddenCard(card.id, card.image))
      case CardRecovered(player, card) if (player == playerId) =>
        CardRecovered(player, VisibleCard(card.id, card.image))
      case other =>
        other
    }
    clockedResponse.copy(event = event)

  }


}
