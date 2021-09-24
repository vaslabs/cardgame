package cardgame.engine

import cats.effect.IO
import cardgame.model._
import StartingGameOps._
import StartedGameOps._
object GameOps {

  implicit final class _GameOps(val game: Game) extends AnyVal {

    def join(player: JoiningPlayer): (Game, Event) = {
      game match {
        case sg: StartingGame if !sg.playersJoined.exists(_.id == player.id) =>
          sg.join(player)
        case sg: StartingGame if
          (sg.playersJoined.exists(_.id == player.id) &&
            sg.playersJoined.exists(_.publicKey.getEncoded sameElements player.publicKey.getEncoded)) =>
          sg.join(player)
        case sg: StartedGame =>
          sg.join(player)
        case _ =>
          game -> InvalidAction(player.id)
      }
    }

    def authorise(authorisationTicket: Authorise): (Game, Event) = {

      game match {
        case sg: StartingGame =>
          game -> sg.playersJoined.find(_.id == authorisationTicket.playerId).map(
            player => AuthorisePlayer(player.id)
          ).getOrElse(InvalidAction(authorisationTicket.playerId))
        case StartedGame(players, _, _, _, _, _) =>
          game -> players.find(_.id == authorisationTicket.playerId).map(
            player => AuthorisePlayer(player.id)
          ).getOrElse(InvalidAction(authorisationTicket.playerId))
        case _ =>
          game -> InvalidAction(authorisationTicket.playerId)
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
          val winner = sg.players.head
          EndedGame(winner) ->(GameFinished(winner.id))
        case sg: StartedGame =>
          ForcedGameEnd(sg.players) -> GameStopped()
      }


    def action(
                gameAction: Action,
                randomizer: IO[Int],
                isIdempotent: PlayerId => (RemoteClock, RemoteClock) => Boolean)(
                  oldClock: RemoteClock,
                  newClock: RemoteClock
    ): (Game, Event) = {
      gameAction match {
        case jg: JoinGame =>
          if (isIdempotent(jg.player.id)(oldClock, newClock))
            join(jg.player)
          else
            game -> InvalidAction(jg.playerId)
        case authorisationTicket: Authorise =>
          authorise(authorisationTicket)
        case EndGame =>
          end
        case StartGame(deck) =>
          start(deck, randomizer)

        case p: PlayingGameAction =>
          if (isIdempotent(p.player)(oldClock, newClock)) {
            game match {
              case sg: StartedGame =>
                sg.playingAction(p, randomizer)
              case _ =>
                game -> InvalidAction(p.player)
            }
          } else
            game -> OutOfSync(p.player)
        case _ =>
          game -> InvalidAction()
      }
    }


  }
}