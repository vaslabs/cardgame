package cardgame.engine

import cardgame.model.{CardId, Deck, PlayingPlayer, VisibleCard}

import scala.collection.mutable
import scala.util.Random

object ShuffleGameAlgorithm {

  def shuffleHand(deck: Deck, players: List[PlayingPlayer]): (Deck, List[PlayingPlayer], List[VisibleCard]) = {
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
        takeFromDeck <- deck.cards.filterNot(c => takenCards.contains(c.id)).find(c => c.cardName == guaranteedCard).toList
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
      c => exclude.contains(c.cardName)
    )

    val discardCards = deckWithoutExclusionCards.filter(c => discard.contains(c.cardName))

    val deckWithoutDiscardedCards = deckWithoutExclusionCards.filterNot(
      c => discard.contains(c.cardName)
    )

    val shuffledCards = Random.shuffle(deckWithoutDiscardedCards)

    val playersWithHand = playerWithGuaranteedCards.zipWithIndex.map {
      case (player, index) =>
        val cards = shuffledCards.slice(index * handSize, index * handSize + handSize)
        takenCards ++= cards.map(_.id).toSet
        player.copy(hand = Random.shuffle(player.hand ++ cards))
    }

    val excludedCards = deck.cards.filter(c => exclude.contains(c.cardName))

    val remainingCards = shuffledCards.filterNot(c => takenCards.contains(c.id))


    (
      Deck(Random.shuffle(excludedCards ++ remainingCards), None, deck.startingRules, deck.pointRules),
      playersWithHand,
      discardCards.map(c => VisibleCard(c.id, c.image))
    )

  }
}
