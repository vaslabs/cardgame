package cardgame.endpoints
import sttp.model.StatusCode
import sttp.tapir._

object JoiningGame {

  val joinPlayer =
    endpoint.in(
      "game" / path[String] / "join"
    ).put
    .in(query[String]("username"))
    .out(statusCode(StatusCode.Accepted))
    .errorOut(statusCode(StatusCode.Forbidden))
}
