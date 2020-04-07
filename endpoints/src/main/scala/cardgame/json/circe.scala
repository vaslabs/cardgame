package cardgame.json

import java.net.URI

import cardgame.model.{CardId, DeckId, Event, Game, HiddenCard, PlayerId}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.syntax._

import scala.util.Try
object circe {

  implicit val playerIdEncoder: Encoder[PlayerId] = Encoder.encodeString.contramap(_.value)
  implicit val playerIdDecoder: Decoder[PlayerId] = Decoder.decodeString.map(PlayerId)
  implicit val cardIdEncoder: Encoder[CardId] = Encoder.encodeUUID.contramap(_.value)
  implicit val cardIdDecoder: Decoder[CardId] = Decoder.decodeUUID.map(CardId)

  implicit val deckIdEncoder: Encoder[DeckId] = Encoder.encodeUUID.contramap(_.value)
  implicit val deckIdDecoder: Decoder[DeckId] = Decoder.decodeUUID.map(DeckId)

  implicit val uriEncoder: Encoder[URI] = Encoder.encodeString.contramap(_.toASCIIString)
  implicit val uriDecoder: Decoder[URI] = Decoder.decodeString.emapTry(s => Try(URI.create(s)))

  implicit val eventEncoder: Encoder[Event] = deriveEncoder
  implicit val eventDecoder: Decoder[Event] = deriveDecoder

  implicit val gameEncoder: Encoder[Game] = deriveEncoder
  implicit val gameDecoder: Decoder[Game] = deriveDecoder

  implicit val hiddenCardEncoder: Encoder[HiddenCard] =
    Encoder.instance {
      hc =>
        Json.obj("id" -> hc.id.asJson)
    }
}
