package cardgame.json

import java.net.URI

import cardgame.model.{CardId, Event, Game, PlayerId}
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto._

import scala.util.Try
object circe {

  implicit val playerIdEncoder: Encoder[PlayerId] = Encoder.encodeString.contramap(_.value)
  implicit val playerIdDecoder: Decoder[PlayerId] = Decoder.decodeString.map(PlayerId)
  implicit val cardIdEncoder: Encoder[CardId] = Encoder.encodeUUID.contramap(_.value)
  implicit val cardIdDecoder: Decoder[CardId] = Decoder.decodeUUID.map(CardId)

  implicit val uriEncoder: Encoder[URI] = Encoder.encodeString.contramap(_.toASCIIString)
  implicit val uriDecoder: Decoder[URI] = Decoder.decodeString.emapTry(s => Try(URI.create(s)))

  implicit val eventEncoder: Encoder[Event] = deriveEncoder
  implicit val eventDecoder: Decoder[Event] = deriveDecoder

  implicit val gameEncoder: Encoder[Game] = deriveEncoder
  implicit val gameDecoder: Decoder[Game] = deriveDecoder
}
