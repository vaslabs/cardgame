package cardgame.processor

import cardgame.model.StartingRules
import cardgame.processor.config.json._
import io.circe.Json
import io.circe.literal.JsonStringContext
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
class ReadStartingRulesSpec extends AnyWordSpec with Matchers {

  "parseable configs" must {
    "accept no gathering pile with single deck" in {
      singleDeckNoGatheringPileRules.as[StartingRules] mustBe
        Right(StartingRules(List.empty, List("card1"), 1, List.empty, false))
    }

    "accept no gathering pile with single deck and discardAll rule" in {
      singleDeckNoGatheringPileRulesDiscardAll.as[StartingRules] mustBe
        Right(StartingRules(List.empty, List("card1"), 1, List("card2"), false))
    }
    "accept gathering pile with single deck and discardAll rule" in {
      singleDeckWithGatheringPileRulesDiscardAll.as[StartingRules] mustBe
        Right(StartingRules(List.empty, List("card1"), 1, List("card2"), true))
    }
  }



  def singleDeckNoGatheringPileRules: Json =
    json"""{
          "exactlyOne": ["card1"],
          "no": [],
          "hand": 1
      }
      """
  def singleDeckNoGatheringPileRulesDiscardAll: Json =
    json"""{
          "exactlyOne": ["card1"],
          "no": [],
          "discardAll": ["card2"],
          "hand": 1
      }
      """

  def singleDeckWithGatheringPileRulesDiscardAll: Json =
    json"""{
          "exactlyOne": ["card1"],
          "no": [],
          "discardAll": ["card2"],
          "gatheringPile": true,
          "hand": 1
      }
      """
}
