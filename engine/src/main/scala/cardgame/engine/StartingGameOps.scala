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
          val players = startingGame.playersJoined
          val startingPlayer = Math.abs(randomizer.unsafeRunSync() % players.size)
          val gamePlayers = players.map(j => PlayingPlayer(j.id, List.empty))
          val startedGame =
            StartedGame(
              gamePlayers,
              deck,
              startingPlayer,
              Clockwise,
              List.empty,
              DiscardPile.empty
            )
          startedGame -> GameStarted(gamePlayers(startingPlayer).id)
    }
  }

}