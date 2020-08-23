package cardgame.processor.config

import cardgame.model.StartingRules
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
object json {

  implicit val configuration: Configuration = Configuration.default.withDefaults

  implicit val startingRulesCodec: Codec[StartingRules] = deriveConfiguredCodec
}
