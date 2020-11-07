package cardgame.events

import cardgame.endpoints.codecs.rsa
import cardgame.json.circe._
import cardgame.model.{ClockedAction, JoiningPlayer, PlayerId, StartingGame}
import io.circe.Json
import io.circe.parser._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
class VerifySignatureSpec extends AnyFlatSpec with Matchers {

  "valid signature" must "be verified" in {
    val payload = """{"Authorise":{"playerId":"vas"},"vectorClock":{"vas":33},"serverClock":10,"signature":"7c6e44254836de518045ff0d68e365c5119d23f1d63308aaac9dda5b98a2a971bd32fe5d4a902ce166309a17b7f1869c3e4af8fe8fe4af34e217d5bc94e9bdff1780e858d6715b69dea9066331e7ef6456dbd89a377841b9202ab572bf20adf08ef749ded02c8e5aef1fcaf80590f6d30c3bfd385eed7ee87342bcb89db64502"}""".stripMargin
    val publicKey =
      "DU1JR2ZNQTBHQ1NxR1NJYjNEUUVCQVFVQUE0R05BRENCaVFLQmdRRHFvZnp0djROTW1nOTk3NTd4NTlzZlhBNXINbG1SY2dieHp3aHV1YWZNSWwxQUdYM2xxbi9ML3FxaUtYQnJjNmpGS3MxejZlU1E0OVlCam1oMmgxaFE0d2k0Wg1Oa3pHNkVBSk15ZS9Tb3ZNbDYrOFNLOTh3aDBuZHp2dG1kRzF5Z0t1cW10bVJndmZQbFZiMW1QUEF1My9hUVdwDWlaUWJCK3Q1VXRPc29rUDZYUUlEQVFBQg0N"

    val signatureValidation = parse(payload).flatMap(_.as[ClockedAction]).map { clockedAction =>
      validateSignature(
        StartingGame(List(JoiningPlayer(PlayerId("vas"), rsa.fromString(publicKey)))),
        clockedAction
      )
    }

    signatureValidation mustBe Right(true)
  }

  "valid join game signature" must "be verified" in {

    val payload = """{"JoinGame":{"player":{"id":"vaslabs","publicKey":"TUlHZk1BMEdDU3FHU0liM0RRRUJBUVVBQTRHTkFEQ0JpUUtCZ1FDRDA5cUhtejFJYVZxaTVBVUsxbisybWdtZUwyVU9nZStKOTRtc2MrRGRqbGtWTXczVzJxVXVYNUhBOUFveW1wWDI1Z2grSWpVRnJuNCtFQ080QUM0czhiTmpNLy9kYWNQK1pWcFJoTVdEcmxKZnZoWkEwSmxrd1R0bmpvR2duQ2hiejF1S2JsZ0dkWjNsZ1JHZkt5c1YyZm9TaDlyTzVuUGhjWFJEd2VhaER3SURBUUFC"}},"vectorClock":{"vaslabs":54},"serverClock":0,"signature":"1473f695935842bf8fe7c396cb39cb5f9eff0d8fb1c1759729f29c86e5de5fa5686677e0f4a3d2f863bc02ef236c3717b78ca4088261c7b34cbb0e67ba1ee3984e1d52dc1dce7cb4e273a01081437aa62a1c587940e3f2320ad99081c611d326e842457603db105bd8867d285bff8a817619304dc4c54c802976076549a6b704"}"""
    val json = parse(payload).getOrElse(Json.Null)
    json.as[ClockedAction].map { action =>
      validateSignature(
        StartingGame(List.empty),
        action
      )
    } mustBe Right(true)

  }

}
