package cardgame.endpoints

import cardgame.model.{Event, GameId, PlayerId}
import sttp.model.StatusCode
import sttp.tapir._
import cardgame.json.circe._
import sttp.tapir.json.circe._

object JoiningGame {

  import codecs.ids._
  import schema.java_types._

  val joinPlayer =
    endpoint.in(
      "game" / path[GameId] / "join"
    ).post
    .in(query[PlayerId]("username"))
    .out(jsonBody[Event])
    .errorOut(statusCode(StatusCode.NotFound))
}
