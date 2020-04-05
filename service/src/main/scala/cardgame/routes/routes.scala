package cardgame.routes

import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import sttp.tapir.server.akkahttp._
import cardgame.endpoints._
import cardgame.processor.ActiveGames.api._
import cardgame.processor.ActiveGames

import scala.concurrent.duration._

class Routes(activeGames: ActorRef[ActiveGames.Protocol])(implicit scheduler: Scheduler) {

  implicit val timeout = Timeout(5 seconds)

  val startingGame =  JoiningGame.joinPlayer.toRoute {
      case (gameId, playerId) => activeGames.joinGame(gameId, playerId)
    } ~ View.gameStatus.toRoute {
      gameId => activeGames.getGame(gameId)
    }

  val adminRoutes = admin.createGame.toRoute {
    token =>  activeGames.createGame(token)
  } ~ admin.startGame.toRoute {
    case (token, gameId, deckId) =>
      activeGames.startGame(token, gameId, deckId)
  }

  val main = startingGame ~ adminRoutes

}

