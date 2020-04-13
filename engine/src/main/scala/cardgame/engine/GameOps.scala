package cardgame.engine

import cats.effect.IO
import cardgame.model._
import StartingGameOps._
import StartedGameOps._
object GameOps {

  implicit final class _GameOps(val game: Game) extends AnyVal {

    def join(player: JoiningPlayer): (Game, Event) = {
      game match {
        case sg: StartingGame =>
          sg.join(player)
        case _ =>
          game -> InvalidAction(player.id)
      }
    }

    def start(deck: Deck, randomizer: IO[Int]): (Game, Event) = {
      game match {
        case sg: StartingGame =>
          sg.start(deck, randomizer)
        case _ =>
          game -> InvalidAction()
      }

    }

    def end: (Game, Event) =
      game match {
        case StartingGame(players) =>
          ForcedGameEnd(players) -> GameStopped()
        case sg: StartedGame if sg.players.size == 1 =>
          val winner = sg.players(0)
          EndedGame(winner) ->(GameFinished(winner.id))
        case sg: StartedGame =>
          ForcedGameEnd(sg.players) -> GameStopped()
      }


    def action(gameAction: Action, randomizer: IO[Int]): (Game, Event) = {
      gameAction match {
        case jg: JoinGame =>
          join(jg.player)
        case EndGame =>
          end
        case StartGame(deck) =>
          start(deck, randomizer)
        case p: PlayingGameAction =>
          game match {
            case sg: StartedGame =>
              sg.playingAction(p, randomizer)
            case _ =>
              game -> InvalidAction(p.player)
          }
        case _ =>
          game -> InvalidAction()
      }
    }


  }
}