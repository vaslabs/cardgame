package cardgame.model

import java.net.URI
import java.security.interfaces.RSAPublicKey
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

case class JoiningPlayer(id: PlayerId, publicKey: RSAPublicKey) extends Player

case class PlayingPlayer(
    id: PlayerId,
    hand: List[Card],
    gatheringPile: GatheringPile,
    points: Long,
    publicKey: RSAPublicKey
) extends Player

case class DeadPlayer(id: PlayerId) extends Player

sealed trait GatheringPile {
  def empty: GatheringPile

  def cards: Set[HiddenCard]
}
case object NoGathering extends GatheringPile {
  def cards: Set[HiddenCard] = Set.empty

  override def empty: GatheringPile = this
}
case class HiddenPile(cards: Set[HiddenCard]) extends GatheringPile {
  override def empty: GatheringPile = HiddenPile(Set.empty)
}

sealed trait Action

sealed trait JoiningGameAction extends Action {
  def playerId: PlayerId
}

case class JoinGame(player: JoiningPlayer) extends JoiningGameAction {
  def playerId = player.id
}
case class Authorise(playerId: PlayerId) extends JoiningGameAction

case class StartGame(deck: Deck) extends Action

sealed trait PlayingGameAction extends Action {
  def player: PlayerId
}

case class ClockedAction(action: Action, vectorClock: Map[String, Long], serverClock: Long, signature: String)
case class AdminAction(action: Action)

sealed trait MustHaveTurnAction extends PlayingGameAction
sealed trait FreeAction extends PlayingGameAction

case class Shuffle(player: PlayerId) extends MustHaveTurnAction
case class DrawCard(player: PlayerId) extends MustHaveTurnAction
case class BottomDraw(player: PlayerId) extends MustHaveTurnAction
case class PlayCard(card: CardId, player: PlayerId) extends MustHaveTurnAction
case class BorrowCard(player: PlayerId, index: Int) extends MustHaveTurnAction
case class ReturnCard(player: PlayerId, cardId: CardId) extends MustHaveTurnAction
case class StealCard(player: PlayerId, from: PlayerId, cardIndex: Int) extends MustHaveTurnAction
case class PutCardBack(card: Card, player: PlayerId, index: Int) extends MustHaveTurnAction
case class GiveCard(card: Card, player: PlayerId, to: PlayerId) extends MustHaveTurnAction
case class SwitchDirection(player: PlayerId) extends MustHaveTurnAction
case class ChooseNextPlayer(player: PlayerId, next: PlayerId) extends MustHaveTurnAction
case class Leave(player: PlayerId) extends MustHaveTurnAction
case class RecoverCard(player: PlayerId, cardId: CardId) extends MustHaveTurnAction
case class EndTurn(player: PlayerId) extends MustHaveTurnAction
case class ThrowDice(player: PlayerId, numberOfDice: Int, sides: Int) extends MustHaveTurnAction
case class GrabCards(player: PlayerId, cards: List[CardId]) extends MustHaveTurnAction
case class ShuffleHand(player: PlayerId) extends FreeAction
case class RestartGame(player: PlayerId) extends MustHaveTurnAction

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
  def cardName: String = {
    val `/` = image.getPath.lastIndexOf("/")
    val `.jpg` = image.getPath.lastIndexOf(".jpg")
    (`/`, `.jpg`) match {
      case (i, j) if (i >= 0 && j >= 0) =>
        val conversion = image.getPath.substring(`/` + 1, `.jpg`)
        conversion
      case _ =>
        image.getPath
    }
  }
}
case class HiddenCard(id: CardId, image: URI) extends Card
case class VisibleCard(id: CardId, image: URI) extends Card

case class CardId(value: UUID)

case class StartingRules(
  no: List[String],
  exactlyOne: List[String],
  hand: Int,
  discardAll: List[String] = List.empty,
  gatheringPile: Boolean = false
)
object StartingRules {
  def empty = StartingRules(List.empty, List.empty, 0, List.empty, false)
}

case class PointCounting(
  cards: Map[String, Int],
  bonus: BonusRule = NoBonus
)

sealed trait BonusRule
case class MostCards(points: Int) extends BonusRule
case object NoBonus extends BonusRule

case class BorrowedCards(playerId: PlayerId, cards: List[Card]) {
  def take(cardId: CardId): Option[BorrowedCards] = {
    if (cards.isEmpty)
      None
    else
      Some(copy(cards = cards.filterNot(_.id == cardId)))
  }
  def put(card: Card, playerId: PlayerId): BorrowedCards =
    copy(playerId, cards :+ card)
}

case class Deck(cards: List[Card], borrowed: Option[BorrowedCards], startingRules: StartingRules, pointRules: Option[PointCounting]) {
  def putBack(card: Card, index: Int): Deck =
    if (index >= cards.size)
      this.copy(cards = cards :+ card)
    else if (index >= 0) {
      this.copy(cards = cards.patch(index, List(card), 0))
    } else
      this


  def borrow(index: Int, playerId: PlayerId): (Deck, Option[Card]) = {
    val card = cards.lift(index)
    card.map {
      c => copy(
        cards.patch(index, Nil, 1),
        borrowed.map(b => b.put(c, playerId)) orElse
          Some(
            BorrowedCards(
              playerId, List(cards(index))
            )
          )
      ) -> Some(c)
    }.getOrElse(this -> None)
  }

  def returnCard(cardId: CardId): Option[Deck] = {
    val toReturn = borrowed.flatMap(b => b.cards.find(_.id == cardId))

    toReturn.map(
      c => copy(c +: cards, borrowed.flatMap(_.take(c.id)))
    )
  }
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
case object Unauthorised extends Event
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
case class OutOfSync(playerId: PlayerId) extends Event
case class DiceThrow(playerId: PlayerId, dice: List[Die]) extends Event
case class ShuffledHand(playerId: PlayerId, hand: List[Card]) extends Event
case class AddedToPile(playerId: PlayerId, cards: Set[VisibleCard]) extends Event
case class GameRestarted(startedGame: StartedGame) extends Event
case class AuthorisePlayer(player: PlayerId) extends Event

case class Die(sides: Int, result: Int)
object InvalidAction {
  def apply(): InvalidAction = InvalidAction(None)
  def apply(playerId: PlayerId): InvalidAction =
    InvalidAction(Some(playerId))
}

sealed trait GameCompleted extends Event

case class GameStopped() extends GameCompleted
case class GameFinished(winner: PlayerId) extends GameCompleted

case class ClockedResponse private (event: Event, clock: Map[String, Long], serverClock: Long)

object ClockedResponse {
  def apply(event: Event, remoteClock: RemoteClock, serverClock: Long): ClockedResponse =
    ClockedResponse(event, remoteClock.showMap, serverClock)
}

case class RemoteClock(vectorClock: Map[PlayerId, Long]) {
  def showMap: Map[String, Long] = vectorClock.map {
    case (key, value) => key.value -> value
  }
}

object RemoteClock {

  def zero = RemoteClock(Map.empty)
  def of(clocks: Map[String, Long]) = RemoteClock(
    clocks.map {
      case (key, value) => PlayerId(key) -> value
    }
  )

}