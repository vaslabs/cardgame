package cardgame.processor

import cardgame.model.PlayerId
import io.circe.{Encoder, KeyEncoder}

object JsonEncoder {
  implicit val playerIdEncoder: Encoder[PlayerId] = Encoder.encodeString.contramap(_.value)
  implicit val playerIdKeyEncoder: KeyEncoder[PlayerId] = KeyEncoder.encodeKeyString.contramap(_.value)
}
