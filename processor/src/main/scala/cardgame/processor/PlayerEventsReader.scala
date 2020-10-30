package cardgame.processor

import java.nio.charset.StandardCharsets
import java.security.Signature
import java.security.interfaces.RSAPublicKey

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model._

object PlayerEventsReader {


  def behavior(playerId: PlayerId, replyTo: ActorRef[ClockedResponse]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
          case AuthorisationTicket(player, plainText, userSignature, publicKey) if player == playerId =>
            val verified = verifySignature(plainText, userSignature, publicKey)
            if (verified) {
              ctx.system.eventStream ! EventStream.Subscribe(ctx.self)
              readingEvents(playerId, replyTo)
            } else {
              Behaviors.stopped
            }

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
    case (_, UserResponse(msg)) =>
      replyTo ! personalise(msg, id)
      Behaviors.same
    case (_, UpdateStreamer(streamer)) =>
      swapConnection(id, replyTo, streamer)
    case (ctx, AuthorisationTicket(player, plainText, userSignature, publicKey)) if player == id =>
      val verified = verifySignature(plainText, userSignature, publicKey)
      if (verified) {
        ctx.system.eventStream ! EventStream.Subscribe(ctx.self)
        readingEvents(id, swapConnectionTo)
      } else {
        readingEvents(id, replyTo)
      }
  }

  private def verifySignature(plainText: String, userSignature: String, publicKey: RSAPublicKey): Boolean = {
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initVerify(publicKey)
    signature.update(plainText.getBytes(StandardCharsets.UTF_8))
    signature.verify(userSignature.getBytes(StandardCharsets.UTF_8))
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
  case class AuthorisationTicket(
                                  player: PlayerId,
                                  plainText: String,
                                  signature: String,
                                  publicKey: RSAPublicKey
                                ) extends Protocol


}
