package cardgame.routes

import java.io.File

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import cardgame.endpoints._
import cardgame.events._
import cardgame.model.PlayerId
import cardgame.processor.ActiveGames
import cardgame.processor.ActiveGames.api._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import sttp.tapir.server.akkahttp._

import scala.concurrent.duration._

class Routes(activeGames: ActorRef[ActiveGames.Protocol])(implicit scheduler: Scheduler, actorContext: ActorContext[_]) {

  implicit val timeout = Timeout(5 seconds)
  private val PlayerIdMatcher = RemainingPath.map(_.toString).map(PlayerId)

  val startingGame =  JoiningGame.joinPlayer.toRoute {
      case (gameId, playerId) => activeGames.joinGame(gameId, playerId)
    } ~ View.gameStatus.toRoute {
      case (gameId, playerId) => activeGames.getGame(gameId, playerId)
    } ~ Actions.player.toRoute {
        case (gameId, action) =>
          activeGames.action(gameId, action)
    } ~ path("events" / PlayerIdMatcher) {
      (playerId) => get {
          complete(toSse(eventSource(playerId)))
       }
    } ~ get {
      path("img" / RemainingPath) {
        imageFile =>
            getFromFile(
              new File(s"decks/${imageFile.toString()}"),
              MediaTypes.`image/jpeg`
            )
      }
    }


  val adminRoutes = admin.createGame.toRoute {
    token =>  activeGames.createGame(token)
  } ~ admin.startGame.toRoute {
    case (token, gameId, deckId, server) =>
      activeGames.startGame(token, gameId, deckId, server)
  }


  val main = cors() {
    startingGame ~ adminRoutes
  }
}

