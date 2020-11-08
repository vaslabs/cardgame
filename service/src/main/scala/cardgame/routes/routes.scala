package cardgame.routes

import java.io.File

import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.typed.scaladsl.ActorSink
import akka.util.Timeout
import cardgame.endpoints.{JoiningGame, _}
import cardgame.events._
import cardgame.json.circe.requests._
import cardgame.model._
import cardgame.processor.ActiveGames
import cardgame.processor.ActiveGames.api._
import cardgame.processor.ActiveGames.{DoGameAction, Ignore}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import io.circe.parser._
import io.circe.syntax._
import sttp.tapir.server.akkahttp._

import scala.concurrent.Future
import scala.concurrent.duration._

class Routes(activeGames: ActorRef[ActiveGames.Protocol])(implicit scheduler: Scheduler) {

  implicit val timeout = Timeout(5 seconds)
  private val PlayerIdMatcher = RemainingPath.map(_.toString).map(PlayerId)

  val websocketRoute = get {
    path("live" / "actions" / PathGameId / PlayerIdMatcher) {
        (gameId, playerId) =>
          handleWebSocketMessages(gameActionsFlow(gameId, playerId))
    }
  }

  val startingGame =  JoiningGame.joinPlayer.toRoute {
      case (gameId, ClockedAction(JoinGame(player), vectorClock, _, signature)) =>
        activeGames.joinGame(gameId, player.id, signature, RemoteClock.of(vectorClock), player.publicKey)
      case _ =>
        Future.successful(Left(()))
    } ~ View.gameStatus.toRoute {
      case (gameId, playerId) => activeGames.getGame(gameId, playerId)
    } ~ get {
    path("img" / RemainingPath) {
      imageFile =>
        getFromFile(
          new File(s"decks/${imageFile.toString()}"),
          MediaTypes.`image/jpeg`
        )
    }
  } ~ websocketRoute


  def gameActionsFlow(gameId: GameId, playerId: PlayerId): Flow[Message, Message, Any] = {
    val source = eventSource(playerId, activeGames).map(_.asJson.noSpaces).map(TextMessage.apply)
    val sink: Sink[Message, Any] = ActorSink.actorRef[ActiveGames.Protocol](activeGames, Ignore, _ => Ignore).contramap[ClockedAction](
      ca => DoGameAction(gameId, ca)
    ).contramap[Message](extractClockedAction)

    Flow.fromSinkAndSource(sink, source)
  }

  private def extractClockedAction(message: Message): ClockedAction =
    parse(message.asTextMessage.getStrictText).flatMap(_.as[ClockedAction]).toOption.get


  private final val PathGameId = JavaUUID.map(GameId)


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

