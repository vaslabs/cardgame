package cardgame.engine

import cardgame.model._
import cats.effect.IO

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
            val (startingDeck, gamePlayers, discardCards) = ShuffleGameAlgorithm.shuffleHand(
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



}