package cardgame.processor

import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import cardgame.model._
import cats.effect.IO
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.immutable.ListMap

class GameProcessorAuthenticationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll{

  import GameProcessorAuthenticationSpec._
  val actorTestKit = ActorTestKit("GameProcessorSpec")
  def generateKeyPair = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(1024)
    kpg.generateKeyPair()
  }


  private val keyPair1 = generateKeyPair
  private val otherKey = generateKeyPair

  val dummySignatureCheck: (Game, ClockedAction) => Boolean = (_, _) => true

  "a joining player" must {
    val joiningPlayer = PlayerId("player1")
    val gameProcessor = actorTestKit.spawn(
      GameProcessor.behavior(startingGame, IO.delay(1), 0, RemoteClock.zero)(dummySignatureCheck)
    )
    val userProbe = actorTestKit.createTestProbe[Either[Unit, ClockedResponse]]()

    "authenticate with an id and a public key" in {
      gameProcessor ! GameProcessor.RunCommand(
        userProbe.ref,
        ClockedAction(
          JoinGame(JoiningPlayer(joiningPlayer, keyPair1.getPublic.asInstanceOf[RSAPublicKey])),
          vectorClock = ListMap("player1" -> 1L),
          0L,
          ""
        )
      )

      userProbe.expectMessageType[Right[Unit, ClockedResponse]].value.event mustBe PlayerJoined(joiningPlayer)
    }

    "a player with the same id but different public key is not accepted" in {
      gameProcessor ! GameProcessor.RunCommand(
        userProbe.ref,
        ClockedAction(
          JoinGame(JoiningPlayer(joiningPlayer, otherKey.getPublic.asInstanceOf[RSAPublicKey])),
          ListMap("player1" -> 1L),
          0L,
          ""
        )
      )

      userProbe.expectMessageType[Right[Unit, ClockedResponse]].value.event mustBe InvalidAction(joiningPlayer)
    }

    "a replay of player successful join is rejected" in {
      gameProcessor ! GameProcessor.RunCommand(
        userProbe.ref,
        ClockedAction(
          JoinGame(JoiningPlayer(joiningPlayer, keyPair1.getPublic.asInstanceOf[RSAPublicKey])),
          ListMap("player1" -> 1L),
          0L,
          "",
        )
      )

      userProbe.expectMessageType[Right[Unit, ClockedResponse]].value.event mustBe InvalidAction(joiningPlayer)
    }

    "a player can rejoin with an updated logical timestamp" in {
      gameProcessor ! GameProcessor.RunCommand(
        userProbe.ref,
        ClockedAction(
          JoinGame(JoiningPlayer(joiningPlayer, keyPair1.getPublic.asInstanceOf[RSAPublicKey])),
          ListMap("player1" -> 2L),
          0L,
          "",
        )
      )

      userProbe.expectMessageType[Right[Unit, ClockedResponse]].value.event mustBe PlayerJoined(joiningPlayer)
    }

  }
}

object GameProcessorAuthenticationSpec {
  def startingGame = StartingGame(
    List.empty
  )
}