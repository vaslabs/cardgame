package cardgame.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import sttp.tapir.server.akkahttp._
import cardgame.endpoints._

import scala.concurrent.Future
object routes {


  val startingGame =  JoiningGame.joinPlayer.toRoute {
      _ => Future.successful(Left(()))
    } ~ get {
      complete(StatusCodes.OK)
    }

}
