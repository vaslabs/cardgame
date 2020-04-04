package cardgame

import java.net.URI
import java.util.UUID

trait Game

case class StartingGame(playersJoined: List[JoiningPlayer]) extends Game

case class StartedGame(
    players: List[PlayingPlayer],
    deck: Deck,
    nextPlayer: Int,
    direction: Direction,
    deadPlayers: List[PlayingPlayer],
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
case class ThrowCard(card: Card, player: PlayerId) extends PlayingGameAction
case class BorrowCard(player: PlayerId) extends PlayingGameAction
case class StealCard(player: PlayerId, from: PlayerId, cardIndex: Int) extends PlayingGameAction
case class AskForCard(card: Card, player: PlayerId, from: Player) extends PlayingGameAction
case class PutCardBack(card: Card, player: PlayerId) extends PlayingGameAction
case class GiveCard(card: Card, player: PlayerId, to: PlayerId) extends PlayingGameAction
case class SwitchDirection(player: PlayerId) extends PlayingGameAction
case class Leave(player: PlayerId) extends PlayingGameAction
case class EndTurn(player: PlayerId) extends PlayingGameAction

case object EndGame extends Action

sealed trait Direction
case object Clockwise extends Direction
case object AntiClockWise extends Direction

sealed trait Card {
  def id: CardId
}
case class HiddenCard(id: CardId, image: URI) extends Card
case class ShownCard(id: CardId, image: URI) extends Card

case class CardId(value: UUID)

case class Deck(cards: List[Card])
case class DiscardPile(cards: List[Card])
object DiscardPile {
  def empty = DiscardPile(List.empty)
}

sealed trait Event
case class PlayerJoined(playerJoined: PlayerId) extends Event
case class GameStarted(startingPlayer: PlayerId) extends Event
case class NextPlayer(player: PlayerId) extends Event
case class GotCard(playerId: PlayerId, cardId: CardId) extends Event
case class DeckShuffled(deck: Deck) extends Event
case class MoveCard(card: Card, from: PlayerId, to: PlayerId) extends Event
case class NewDirection(direction: Direction) extends Event
case class PlayerLeft(player: PlayerId) extends Event
case object InvalidAction extends Event

case object GameStopped extends Event
case class GameFinished(winner: PlayerId) extends Event

