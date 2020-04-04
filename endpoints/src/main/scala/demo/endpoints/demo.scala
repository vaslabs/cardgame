package demo.endpoints

import sttp.tapir._
import io.circe.generic.auto._
import sttp.tapir.json.circe._
import _root_.demo.model._
import _root_.demo.json.circe._

object demo {


  val additionResult = stringBody("utf-8").example("7").map(_.toInt)(_.toString)

  val http = endpoint
    .get
    .in(query[Int]("a").description("The first number to add").example(4))
    .in(query[Int]("b").description("The first number to add").example(3))
    .out(additionResult)


  val addition = endpoint.post
    .in(jsonBody[Addition])
    .out(jsonBody[Sum])

}
