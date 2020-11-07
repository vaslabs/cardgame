package cardgame.endpoints

import cardgame.json.circe._
import cardgame.model.{ClockedAction, ClockedResponse, GameId}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
object JoiningGame {

  import cardgame.endpoints.codecs.ids._
  import schema.vector_clock._

  val joinPlayer =
    endpoint.in(
      "game" / path[GameId] / "join"
    ).post
    .in(jsonBody[ClockedAction])
    .out(jsonBody[ClockedResponse])
    .errorOut(statusCode(StatusCode.NotFound))
}
