package cardgame

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import cardgame.routes.{Documentation, routes}
import akka.http.scaladsl.server.Directives._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import scala.io.StdIn

object Bootstrap extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = Materializer(system)
  implicit val executionContext = system.dispatcher

  val documentationRoute =
    new SwaggerAkka(Documentation.openApiYaml).routes



  val bindingFuture = Http().bindAndHandle(
    documentationRoute ~ routes.startingGame, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
