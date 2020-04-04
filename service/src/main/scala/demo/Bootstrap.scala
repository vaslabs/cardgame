package demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer

import scala.io.StdIn
import akka.http.scaladsl.server.Directives._

object Bootstrap extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = Materializer(system)
  implicit val executionContext = system.dispatcher

  val bindingFuture = Http().bindAndHandle(routes.documentationRoute ~ routes.additionRoute, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
