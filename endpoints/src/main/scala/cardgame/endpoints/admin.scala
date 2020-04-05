package cardgame.endpoints
import java.util.UUID

import sttp.model.StatusCode
import sttp.tapir._
object admin {

  val createGame =
    endpoint.in("games-admin").post
    .in(auth.bearer)
    .out(stringBody("utf-8").map(UUID.fromString)(_.toString))
    .errorOut(statusCode(StatusCode.Forbidden))
}
