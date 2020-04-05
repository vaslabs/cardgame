package cardgame.endpoints
import java.net.URI

import cardgame.model.{Event, GameId, PlayerId}
import sttp.model.StatusCode
import sttp.tapir._
import cardgame.json.circe._
import sttp.tapir.json.circe._
object JoiningGame {

  implicit val gameIdCodec = Codec.uuidPlainCodec
    .map(GameId)(_.value)

  implicit val schemaForURI = Schema[URI](SchemaType.SString)

  implicit val playerId = Codec.stringPlainCodecUtf8.map(
    PlayerId
  )(_.value)

  val joinPlayer =
    endpoint.in(
      "game" / path[GameId] / "join"
    ).put
    .in(query[PlayerId]("username"))
    .out(jsonBody[Event])
    .errorOut(statusCode(StatusCode.NotFound))
}
