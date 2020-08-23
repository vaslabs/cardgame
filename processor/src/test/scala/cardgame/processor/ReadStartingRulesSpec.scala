package cardgame.processor

import cardgame.model.{MostCards, NoBonus, PointCounting, StartingRules}
import cardgame.processor.config.json._
import io.circe.Json
import io.circe.literal.JsonStringContext
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
class ReadStartingRulesSpec extends AnyWordSpec with Matchers {

  "parseable starting rules config" must {
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

  "parseable point counting config" must {
    "accept points per card and bonus" in {
      pointsConfig.as[PointCounting] mustBe Right(PointCounting(
        Map("J_h" -> 1, "J_s" -> 1, "10_h" -> 3),
        MostCards(3)
      ))
    }
    "accept points per card without bonus" in {
      noBonusPointsConfig.as[PointCounting] mustBe Right(PointCounting(
        Map("J_h" -> 1, "J_s" -> 1, "10_h" -> 3),
        NoBonus
      ))
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

  def pointsConfig: Json =
    json"""{
      "cards": {
        "J_h": 1,
        "J_s": 1,
        "10_h": 3
      },
      "bonus": {
          "mostCards": 3
      }
    }
    """

  def noBonusPointsConfig: Json =
    json"""{
      "cards": {
        "J_h": 1,
        "J_s": 1,
        "10_h": 3
      }
    }
    """
}
