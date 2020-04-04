package demo
import akka.http.scaladsl.model.StatusCodes
import demo.endpoints.AdditionOp
import sttp.tapir.server.akkahttp._
import demo.endpoints.demo._

import scala.concurrent.Future
import akka.http.scaladsl.server.Directives._
import sttp.tapir.swagger.akkahttp.SwaggerAkka
import demo.model._

object routes {


  val additionRoute = http.toRoute {
    case (a,b) => Future.successful(Right(AdditionOp.op(a, b)))
  } ~ addition.toRoute {
    case Addition(a,b) => Future.successful(Right(Sum(AdditionOp.op(a, b))))
  } ~ path("health") {
    get {
      complete(StatusCodes.OK)
    }
  }

  val documentationRoute = new SwaggerAkka(Documentation.openApiYaml).routes


}
