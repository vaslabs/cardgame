package cardgame.endpoints

import cardgame.model.{Game, GameId}
import sttp.model.StatusCode
import sttp.tapir._
import cardgame.json.circe._
import sttp.tapir.json.circe._

object View {
  import codecs.ids._
  import schema.java_types._

  val gameStatus =
    endpoint.get.in(
      "game" / path[GameId]
    ).out(jsonBody[Game])
    .errorOut(statusCode(StatusCode.NotFound))

}
