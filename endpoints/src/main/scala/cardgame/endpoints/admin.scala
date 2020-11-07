package cardgame.endpoints
import java.util.UUID

import cardgame.model.{DeckId, GameId}
import sttp.model.StatusCode
import sttp.tapir._
import cardgame.endpoints.codecs.ids._

object admin {

  val createGame =
    endpoint.in("games-admin").post
    .in(auth.bearer[String])
    .out(stringBody("utf-8").map(GameId compose UUID.fromString)(_.value.toString))
    .errorOut(statusCode(StatusCode.Forbidden))

  val startGame =
    endpoint.in("games-admin")
    .patch
    .in(auth.bearer[String])
    .in(query[GameId]("game"))
    .in(query[DeckId]("deck"))
    .in(query[String]("server"))
    .out(statusCode(StatusCode.Ok))
    .errorOut(statusCode(StatusCode.Forbidden))
}
