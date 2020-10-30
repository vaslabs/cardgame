package cardgame.endpoints

import java.util.Base64

import cardgame.model.{ClockedResponse, GameId, PlayerId}
import sttp.model.StatusCode
import sttp.tapir._
import cardgame.json.circe._
import sttp.tapir.json.circe._
import sun.security.rsa.RSAPublicKeyImpl

object JoiningGame {

  import codecs.ids._
  import schema.vector_clock._

  val joinPlayer =
    endpoint.in(
      "game" / path[GameId] / "join"
    ).post
    .in(query[PlayerId]("username"))
      .in(
        auth.bearer
          .map(
            value =>
              RSAPublicKeyImpl.newKey(Base64.getDecoder.decode(value))
          )(rsaKey =>
            Base64.getEncoder.encodeToString(rsaKey.getEncoded)
          )
      )
    .out(jsonBody[ClockedResponse])
    .errorOut(statusCode(StatusCode.NotFound))
}
