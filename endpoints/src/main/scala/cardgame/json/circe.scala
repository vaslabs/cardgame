package cardgame.json

import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.Base64

import cardgame.model.{Action, CardId, ClockedAction, ClockedResponse, DeckId, Event, Game, HiddenCard, PlayerId}
import io.circe.{Codec, Decoder, Encoder, Json, KeyDecoder, KeyEncoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.syntax._
import cats.implicits._
import sun.security.rsa.RSAPublicKeyImpl

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

  implicit val rsaPublicKeyEncoder: Encoder[RSAPublicKey] = Encoder.encodeString.contramap {
    rsaPublicKey =>
      Base64.getEncoder.encodeToString(rsaPublicKey.getEncoded)
  }

  implicit val rsaPublicKeyDecoder: Decoder[RSAPublicKey] = Decoder.decodeString.emapTry {
    value =>
      Try(RSAPublicKeyImpl.newKey(Base64.getDecoder.decode(value)))
  }

  implicit val playerIdKeyEncoder: KeyEncoder[PlayerId] = KeyEncoder.encodeKeyString.contramap(_.value)
  implicit val playerIdKeyDecoder: KeyDecoder[PlayerId] = KeyDecoder.decodeKeyString.map(PlayerId)

  implicit val hiddenCardEncoder: Encoder[HiddenCard] =
    Encoder.instance {
      hc =>
        Json.obj("id" -> hc.id.asJson)
    }

  implicit val playingGameActionCodec: Codec[Action] =
    deriveCodec

  implicit val clockedResponseEncoder: Encoder[ClockedResponse] = Encoder.instance {
    clockedResponse =>
      clockedResponse.event.asJson.mapObject(_.add("vectorClock", clockedResponse.clock.asJson)
          .add("serverClock", clockedResponse.serverClock.asJson)
      )
  }

  implicit val clockedResponseDecoder: Decoder[ClockedResponse] = Decoder.instance {
    hcursor =>
      (
        hcursor.downField("vectorClock").as[Map[String, Long]].orElse(Right(Map.empty[String, Long])),
        hcursor.downField("serverClock").as[Long],
        hcursor.as[Event]
      ).mapN((vectorClocks, serverClock, events) =>
        ClockedResponse(events, vectorClocks, serverClock)
      )
  }

  implicit val clockedActionEncoder: Encoder[ClockedAction] = Encoder.instance {
    clockedAction =>
      clockedAction.action.asJson.mapObject(_.add("vectorClock", clockedAction.vectorClock.asJson)
        .add("serverClock", clockedAction.serverClock.asJson)
      )

  }

  implicit val clockedActionDecoder: Decoder[ClockedAction] = Decoder.instance {
    hcursor =>
      (
        hcursor.downField("vectorClock").as[Map[String, Long]].orElse(Right(Map.empty[String, Long])),
        hcursor.downField("serverClock").as[Long].orElse(Right(0L)),
        hcursor.downField("signature").as[String],
        hcursor.as[Action]
      ).mapN(
        (clock, serverClock, signature, action) => ClockedAction(action, clock, serverClock, signature)
      )
  }
}
