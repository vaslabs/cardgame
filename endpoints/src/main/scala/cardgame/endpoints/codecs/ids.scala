package cardgame.endpoints.codecs

import cardgame.model.{DeckId, GameId, PlayerId}
import sttp.tapir.{Codec, CodecFormat}

object ids {
  implicit val gameIdCodec: Codec[String, GameId, CodecFormat.TextPlain] = Codec.uuid
    .map(GameId)(_.value)

  implicit val deckIdCodec: Codec[String, DeckId, CodecFormat.TextPlain] = Codec.uuid.map(DeckId)(_.value)

  implicit val playerId: Codec[String, PlayerId, CodecFormat.TextPlain] = Codec.string.map(
    PlayerId
  )(_.value)
}