package demo.json

import io.circe.{Decoder, Encoder, Json}
import demo.model._
import io.circe.syntax._

import io.circe.generic.semiauto._

object circe {



  implicit val sumEncoder: Encoder[Sum] = Encoder.instance[Sum] {
    sum =>
      Json.obj("sum" -> sum.value.asJson)
  }

  private val sumFallbackDecoder: Decoder[Sum] = deriveDecoder

  implicit val sumDecoder: Decoder[Sum] = Decoder.instance {
    hcursor =>
      hcursor.downField("sum").as[Int].map(Sum)
  } or sumFallbackDecoder


}
