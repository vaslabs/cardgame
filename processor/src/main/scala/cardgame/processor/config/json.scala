package cardgame.processor.config

import cardgame.model.{BonusRule, MostCards, NoBonus, PointCounting, StartingRules}
import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.syntax._
object json {

  implicit val configuration: Configuration = Configuration.default.withDefaults

  implicit val startingRulesCodec: Codec[StartingRules] = deriveConfiguredCodec

  implicit val bonusRuleEncoder: Encoder[BonusRule] = Encoder.encodeJson.contramap {
    case NoBonus => Json.obj()
    case MostCards(points) =>
      Json.obj("mostCards" -> points.asJson)
  }

  implicit val bonusRuleDecoder: Decoder[BonusRule] = Decoder.instance {
    cursor =>
      cursor.downField("mostCards").as[Int].map(MostCards).orElse(Right(NoBonus))
  }

  implicit val pointCountingCodec: Codec[PointCounting] = deriveConfiguredCodec
}
