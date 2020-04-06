package cardgame.routes

import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import sttp.tapir.server.akkahttp._
import cardgame.endpoints._
import cardgame.model.GameId
import cardgame.processor.ActiveGames.api._
import cardgame.processor.ActiveGames
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import cardgame.events._
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._

import scala.concurrent.duration._

class Routes(activeGames: ActorRef[ActiveGames.Protocol])(implicit scheduler: Scheduler) {

  implicit val timeout = Timeout(5 seconds)
  private val GameUUID = JavaUUID.map(GameId)

  val startingGame =  JoiningGame.joinPlayer.toRoute {
      case (gameId, playerId) => activeGames.joinGame(gameId, playerId)
    } ~ View.gameStatus.toRoute {
      gameId => activeGames.getGame(gameId)
    } ~ path("events" / GameUUID) {
       _ => get {
        extractActorSystem { actorSystem =>
          complete(toSse(eventSource(actorSystem.toTyped)))
        }
       }
    }


  val adminRoutes = admin.createGame.toRoute {
    token =>  activeGames.createGame(token)
  } ~ admin.startGame.toRoute {
    case (token, gameId, deckId) =>
      activeGames.startGame(token, gameId, deckId)
  }


  val main = cors() {
    startingGame ~ adminRoutes
  }
}

