package cardgame.endpoints.codecs

import cardgame.json.circe._
import cardgame.model.{Authorise, ClockedAction, PlayerId}
import io.circe.parser._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
class ActionJsonSpec extends AnyFlatSpec with Matchers {

  "authorise player" must "be decoded" in {
    parse(
    """{"Authorise":{"playerId":"vaslabs"},"vectorClock":{"vaslabs":6},"serverClock":2,"signature":"25c16f74022d78ff319884426602b562ec0a1380f08fd87925a82e30abcf6974608e994e81f8b83a4f0be9d225206caf26dc84deb5dacc9009190d45f4aa61fd589c4c46d488a161cf3af01f5b1d3bfa8370f0e537533d398f17fa61cad918680c272bde5130375bd9b4e97a8fbede92154bc25395f770e13a0a38585f900db2"}"""
    ).flatMap(_.as[ClockedAction]).map(_.action) mustBe Right(Authorise(PlayerId("vaslabs")))
  }

}
