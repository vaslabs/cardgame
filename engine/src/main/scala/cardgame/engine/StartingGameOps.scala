package cardgame.engine

import java.net.URI

import cardgame.model._
import cats.effect.IO

import scala.collection.mutable
import scala.util.Random

object StartingGameOps {

  implicit final class _StartingGameOps(val startingGame: StartingGame) {
    def join(player: JoiningPlayer): (Game, Event) = {
          if (startingGame.playersJoined.contains(player))
            startingGame -> PlayerJoined(player.id)
          else
            StartingGame(startingGame.playersJoined :+ player) -> PlayerJoined(player.id)
    }

    def start(deck: Deck, randomizer: IO[Int]): (Game, Event) = {
          if (startingGame.playersJoined.nonEmpty) {
            val players = startingGame.playersJoined
            val startingPlayer = Math.abs(randomizer.unsafeRunSync() % players.size)
            val (startingDeck, gamePlayers, discardCards) = shuffleHand(
              deck,
              players.map(j =>
                PlayingPlayer(
                  j.id, List.empty, gatheringPile(deck.startingRules.gatheringPile), 0
                )
              )
            )
            val startedGame =
              StartedGame(
                gamePlayers,
                startingDeck,
                startingPlayer,
                Clockwise,
                List.empty,
                DiscardPile(discardCards)
              )
            startedGame -> GameStarted(gamePlayers(startingPlayer).id)
          } else {
            startingGame -> InvalidAction(PlayerId("admin"))
          }
    }
  }

  private val gatheringPile: Boolean => GatheringPile = {
    case true => HiddenPile(Set.empty)
    case _ => NoGathering
  }

  def shuffleHand(deck: Deck, players: List[PlayingPlayer]): (Deck, List[PlayingPlayer], List[Card]) = {
    def cardName(image: URI): String = {
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

    val exactlyOneCards = deck.startingRules.exactlyOne
    val exclude = deck.startingRules.no
    val discard = deck.startingRules.discardAll
    val handSize = deck.startingRules.hand

    val takenCards = mutable.HashSet.empty[CardId]


    val playerWithGuaranteedCards = if (exactlyOneCards.isEmpty)
      players
    else {
      val playerHands = for {
        player <- players
        guaranteedCard <- exactlyOneCards
        takeFromDeck <- deck.cards.filterNot(c => takenCards.contains(c.id)).find(c => cardName(c.image) == guaranteedCard).toList
        _ = takenCards.add(takeFromDeck.id)
        playerWithGuaranteedCards = player.copy(hand = player.hand :+ takeFromDeck)
      } yield playerWithGuaranteedCards

      playerHands.groupBy(_.id).map {
        case (id, player) =>
          player.reduce[PlayingPlayer] {
            case (p1, p2) =>
              PlayingPlayer(id, p1.hand ++ p2.hand, p1.gatheringPile, p1.points)
          }
      }.toList
    }

    val deckWithoutGuaranteedCards = deck.cards.filterNot(c => takenCards.contains(c.id))

    val deckWithoutExclusionCards = deckWithoutGuaranteedCards.filterNot(
      c => exclude.contains(cardName(c.image))
    )

    val discardCards = deckWithoutExclusionCards.filter(c => discard.contains(cardName(c.image)))

    val deckWithoutDiscardedCards = deckWithoutExclusionCards.filterNot(
      c => discard.contains(cardName(c.image))
    )

    val shuffledCards = Random.shuffle(deckWithoutDiscardedCards)

    val playersWithHand = playerWithGuaranteedCards.zipWithIndex.map {
      case (player, index) =>
        val cards = shuffledCards.slice(index * handSize, index * handSize + handSize)
        takenCards ++= cards.map(_.id).toSet
        player.copy(hand = Random.shuffle(player.hand ++ cards))
    }

    val excludedCards = deck.cards.filter(c => exclude.contains(cardName(c.image)))

    val remainingCards = shuffledCards.filterNot(c => takenCards.contains(c.id))


    (
      Deck(Random.shuffle(excludedCards ++ remainingCards), None, deck.startingRules),
      playersWithHand,
      discardCards.map(c => VisibleCard(c.id, c.image))
    )

  }

}