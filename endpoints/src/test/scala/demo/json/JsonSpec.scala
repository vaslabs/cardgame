package demo.json

import demo.json.circe._
import demo.model.Sum
import io.circe.Json
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class JsonSpec extends AnyFlatSpec with Matchers {


  "json encoding" must "be isomorphic with decoding" in {

    val sum: Sum = Sum(2)

    val json = sum.asJson

    val decodedSum = json.as[Sum]

    decodedSum mustBe Right(sum)

  }

  "json decoding" must "also work with value field" in {
    Json.obj("value" -> Json.fromInt(2)).as[Sum] mustBe Right(Sum(2))
  }

}
