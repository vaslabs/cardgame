package cardgame.endpoints

import cardgame.model.{Event, GameId, PlayingGameAction}
import sttp.tapir._
import cardgame.json.circe._
import sttp.model.StatusCode
import sttp.tapir.json.circe._
object Actions {

  import cardgame.endpoints.codecs.ids._
  import schema.java_types._

  val player =
    endpoint.in("action" / path[GameId])
    .in(jsonBody[PlayingGameAction])
    .out(jsonBody[Event])
    .post
    .errorOut(statusCode(StatusCode.NotFound))

}
