package cardgame.endpoints
import cardgame.model.{GameId, PlayerId}
import sttp.model.StatusCode
import sttp.tapir._

object JoiningGame {

  implicit val gameIdCodec = Codec.uuidPlainCodec
    .map(GameId)(_.value)


  implicit val playerId = Codec.stringPlainCodecUtf8.map(
    PlayerId
  )(_.value)


  val joinPlayer =
    endpoint.in(
      "game" / path[GameId] / "join"
    ).put
    .in(query[PlayerId]("username"))
    .out(statusCode(StatusCode.Accepted))
    .errorOut(statusCode(StatusCode.NotFound))
}
