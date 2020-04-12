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
            val (startingDeck, gamePlayers) = shuffleHand(deck, players.map(j => PlayingPlayer(j.id, List.empty)))
            val startedGame =
              StartedGame(
                gamePlayers,
                startingDeck,
                startingPlayer,
                Clockwise,
                List.empty,
                DiscardPile.empty
              )
            startedGame -> GameStarted(gamePlayers(startingPlayer).id)
          } else {
            startingGame -> InvalidAction(PlayerId("admin"))
          }
    }
  }

  def shuffleHand(deck: Deck, players: List[PlayingPlayer]): (Deck, List[PlayingPlayer]) = {
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
    val handSize = deck.startingRules.hand

    val guaranteed = deck.cards.filter(
      c =>
        exactlyOneCards.contains(cardName(c.image))
    ).take(players.size)
    if (guaranteed.size < players.size && guaranteed.nonEmpty) {
      println("players are more than the guaranteed cards, will not give starting hands")
      deck -> players
    } else {

      val takenCards = mutable.HashSet.empty[CardId]

      val playersWithGuaranteedCard = if (guaranteed.isEmpty)
        players else
          players.lazyZip(guaranteed).map {
          case (player, card) =>
            takenCards += card.id
            player.copy(hand = player.hand :+ card)
      }

      val deckWithoutGuaranteedCards = deck.cards.filterNot(
        c => takenCards.contains(c.id)
      )

      val deckWithoutExclusionCards = deckWithoutGuaranteedCards.filterNot(
        c =>
          exclude.contains(cardName(c.image))
      )


      val shuffledCards = Random.shuffle(deckWithoutExclusionCards)

      val playersWithHand = playersWithGuaranteedCard.zipWithIndex.map {
        case (player, index) =>
          val cards = shuffledCards.slice(index * handSize, index * handSize + handSize)
          takenCards ++= cards.map(_.id).toSet
          player.copy(hand = Random.shuffle(player.hand ++ cards))
      }
      println(playersWithGuaranteedCard)

      Deck(Random.shuffle(deck.cards.filterNot(c => takenCards.contains(c.id))), None, deck.startingRules) ->
        playersWithHand
    }



  }

}