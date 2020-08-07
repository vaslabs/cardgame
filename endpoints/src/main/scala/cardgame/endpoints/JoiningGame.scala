package cardgame.endpoints

import cardgame.model.{ClockedResponse, GameId, PlayerId}
import sttp.model.StatusCode
import sttp.tapir._
import cardgame.json.circe._
import sttp.tapir.json.circe._

object JoiningGame {

  import codecs.ids._
  import schema.vector_clock._

  val joinPlayer =
    endpoint.in(
      "game" / path[GameId] / "join"
    ).post
    .in(query[PlayerId]("username"))
    .out(jsonBody[ClockedResponse])
    .errorOut(statusCode(StatusCode.NotFound))
}
