package cardgame.endpoints

import cardgame.json.circe._
import cardgame.model.{ClockedAction, ClockedResponse, GameId}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
object Actions {

  import cardgame.endpoints.codecs.ids._
  import schema.vector_clock._


  val player =
    endpoint.in("action" / path[GameId])
    .in(jsonBody[ClockedAction])
    .out(jsonBody[ClockedResponse])
    .post
    .errorOut(statusCode(StatusCode.NotFound))

}
