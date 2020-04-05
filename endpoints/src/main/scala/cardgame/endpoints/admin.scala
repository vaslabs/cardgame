package cardgame.endpoints
import java.util.UUID

import cardgame.model.GameId
import sttp.model.StatusCode
import sttp.tapir._
object admin {

  val createGame =
    endpoint.in("games-admin").post
    .in(auth.bearer)
    .out(stringBody("utf-8").map(GameId compose UUID.fromString)(_.value.toString))
    .errorOut(statusCode(StatusCode.Forbidden))
}
