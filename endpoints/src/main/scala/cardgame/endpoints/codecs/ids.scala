package cardgame.endpoints.codecs

import cardgame.model.{DeckId, GameId, PlayerId}
import sttp.tapir.Codec

object ids {
  implicit val gameIdCodec = Codec.uuidPlainCodec
    .map(GameId)(_.value)

  implicit val deckIdCodec = Codec.uuidPlainCodec.map(DeckId)(_.value)

  implicit val playerId = Codec.stringPlainCodecUtf8.map(
    PlayerId
  )(_.value)
}
