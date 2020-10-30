package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model._

object PlayerEventsReader {


  def behavior(playerId: PlayerId, replyTo: ActorRef[ClockedResponse]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
        case UserResponse(ClockedResponse(AuthorisePlayer(player), _, _)) if playerId == player =>
          ctx.system.eventStream ! EventStream.Subscribe(ctx.self)
          readingEvents(player, replyTo)
        case _ =>
          Behaviors.same
      }
  }

  private def readingEvents(id: PlayerId, replyTo: ActorRef[ClockedResponse]): Behavior[Protocol] = Behaviors.receiveMessage {
    case UserResponse(msg) =>
      replyTo ! personalise(msg, id)
      Behaviors.same
    case UpdateStreamer(streamer) =>
      swapConnection(id, replyTo, streamer)
    case _ =>
      Behaviors.same
  }

  private def swapConnection(
                              id: PlayerId,
                              replyTo: ActorRef[ClockedResponse],
                              swapConnectionTo: ActorRef[ClockedResponse]): Behavior[Protocol] = Behaviors.receive {

    case (ctx, UserResponse(ClockedResponse(AuthorisePlayer(player), _, _))) if id == player =>
      ctx.system.eventStream ! EventStream.Subscribe(ctx.self)
      readingEvents(id, swapConnectionTo)
    case (_, UserResponse(msg)) =>
      replyTo ! personalise(msg, id)
      Behaviors.same
    case (_, UpdateStreamer(streamer)) =>
      swapConnection(id, replyTo, streamer)
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
      case CardRecovered(player, card) if player == playerId =>
        CardRecovered(player, VisibleCard(card.id, card.image))
      case GameRestarted(startedGame) =>
        GameRestarted(personaliseGame(playerId, startedGame))
      case other =>
        other
    }
    clockedResponse.copy(event = event)

  }

  def personaliseGame(playerId: PlayerId, game: Game): Game = game match {
    case g: StartedGame =>
      personaliseGame(playerId, g)
    case other => other
  }

  def personaliseGame(playerId: PlayerId, game: StartedGame): StartedGame = game match {
    case g @ StartedGame(players, deck, _, _, _, _) =>
      players.indexWhere(_.id == playerId) match {
        case n if n >= 0 =>
          g.copy(players.updated(n, turnVisible(players(n))), turnVisible(deck, playerId))
        case _ => game
      }
  }


  private def turnVisible(player: PlayingPlayer): PlayingPlayer =
    player.copy(hand = player.hand.map(c => VisibleCard(c.id, c.image)))

  private def turnVisible(deck: Deck, playerId: PlayerId): Deck =
    deck.borrowed.filter(
      b => b.playerId == playerId
    ).map(b => b.cards.map(c => VisibleCard(c.id, c.image)))
      .map(vc => deck.copy(borrowed = Some(BorrowedCards(playerId, vc))))
      .getOrElse(deck)

  sealed trait Protocol
  case class UserResponse(clockedResponse: ClockedResponse) extends Protocol
  case class UpdateStreamer(streamer: ActorRef[ClockedResponse]) extends Protocol
  case class AuthorisationTicket(player: PlayerId) extends Protocol


}
