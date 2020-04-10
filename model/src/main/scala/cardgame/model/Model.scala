package cardgame.model

import java.net.URI
import java.util.UUID

case class DeckId(value: UUID)
case class GameId(value: UUID)

sealed trait Game

case class StartingGame(playersJoined: List[JoiningPlayer]) extends Game

case class StartedGame(
                        players: List[PlayingPlayer],
                        deck: Deck,
                        nextPlayer: Int,
                        direction: Direction,
                        deadPlayers: List[DeadPlayer],
                        discardPile: DiscardPile
                      ) extends Game

sealed trait FinishedGame extends Game

case class EndedGame(winner: Player) extends FinishedGame
case class ForcedGameEnd(remainingPlayers: List[Player]) extends FinishedGame

case class PlayerId(value: String)
sealed trait Player {
  def id: PlayerId
}

case class JoiningPlayer(id: PlayerId) extends Player

case class PlayingPlayer(id: PlayerId, hand: List[Card]) extends Player

case class DeadPlayer(id: PlayerId) extends Player

sealed trait Action

sealed trait JoiningGameAction extends Action

case class JoinGame(player: JoiningPlayer) extends JoiningGameAction

case class StartGame(deck: Deck) extends Action

sealed trait PlayingGameAction extends Action {
  def player: PlayerId
}

case class Shuffle(player: PlayerId) extends PlayingGameAction
case class DrawCard(player: PlayerId) extends PlayingGameAction
case class BottomDraw(player: PlayerId) extends PlayingGameAction
case class PlayCard(card: CardId, player: PlayerId) extends PlayingGameAction
case class BorrowCard(player: PlayerId, index: Int) extends PlayingGameAction
case class ReturnCard(player: PlayerId, cardId: CardId) extends PlayingGameAction
case class StealCard(player: PlayerId, from: PlayerId, cardIndex: Int) extends PlayingGameAction
case class PutCardBack(card: Card, player: PlayerId, index: Int) extends PlayingGameAction
case class GiveCard(card: Card, player: PlayerId, to: PlayerId) extends PlayingGameAction
case class SwitchDirection(player: PlayerId) extends PlayingGameAction
case class ChooseNextPlayer(player: PlayerId, next: PlayerId) extends PlayingGameAction
case class Leave(player: PlayerId) extends PlayingGameAction
case class RecoverCard(player: PlayerId, cardId: CardId) extends PlayingGameAction
case class EndTurn(player: PlayerId) extends PlayingGameAction

case object EndGame extends Action

sealed trait Direction {
  def reverse: Direction
}
case object Clockwise extends Direction {
  override def reverse: Direction = AntiClockwise
}
case object AntiClockwise extends Direction {
  override def reverse: Direction = Clockwise
}

sealed trait Card {
  def id: CardId
  def image: URI
}
case class HiddenCard(id: CardId, image: URI) extends Card
case class VisibleCard(id: CardId, image: URI) extends Card

case class CardId(value: UUID)

case class Deck(cards: List[Card], borrowed: List[Card], startingRules: Map[String, List[String]]) {
  def putBack(card: Card, index: Int): Deck =
    if (index >= cards.size)
      Deck(cards :+ card)
    else if (index >= 0) {
      Deck(cards.patch(index, List(card), 0))
    } else
      this


  def borrow(index: Int): Deck =
    Deck(cards.patch(index, Nil, 1), borrowed :+ cards(index))

  def returnCard(cardId: CardId): Option[Deck] = {
    val toReturn = borrowed.find(_.id == cardId)
    toReturn.map(
      c => Deck(c +: cards, borrowed.filterNot(_.id == cardId))
    )
  }
}

object Deck {
  def apply(cards: List[Card]): Deck = Deck(cards, List.empty, Map.empty)

  def apply(cards: List[Card], borrowed: List[Card]): Deck = Deck(cards, borrowed, Map.empty)


}

case class DiscardPile(cards: List[Card])
object DiscardPile {
  def empty = DiscardPile(List.empty)
}

sealed trait Event
case class PlayerJoined(id: PlayerId) extends Event
case class GameStarted(startingPlayer: PlayerId) extends Event
case class NextPlayer(player: PlayerId) extends Event
case class GotCard(playerId: PlayerId, card: Card) extends Event
case class BorrowedCard(card: Card, playerId: PlayerId) extends Event
case class ReturnedCard(card: CardId, index: Int) extends Event
case class BackToDeck(card: Card, index: Int) extends Event
case class DeckShuffled(deck: Deck) extends Event
case class PlayedCard(card: VisibleCard, playerId: PlayerId) extends Event
case class MoveCard(card: Card, from: PlayerId, to: PlayerId) extends Event
case class NewDirection(direction: Direction) extends Event
case class PlayerLeft(player: PlayerId, nextCurrentPlayer: Int) extends Event
case class CardRecovered(player: PlayerId, card: Card) extends Event
case class InvalidAction(playerId: Option[PlayerId]) extends Event

object InvalidAction {
  def apply(): InvalidAction = InvalidAction(None)
  def apply(playerId: PlayerId): InvalidAction =
    InvalidAction(Some(playerId))
}

sealed trait GameCompleted extends Event

case class GameStopped() extends GameCompleted
case class GameFinished(winner: PlayerId) extends GameCompleted
