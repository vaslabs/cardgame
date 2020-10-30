package cardgame.processor

import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import cardgame.model.{ClockedResponse, InvalidAction, JoinGame, JoiningPlayer, PlayerId, PlayerJoined, RemoteClock, StartingGame}
import cats.effect.IO
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

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

  "a joining player" must {
    val joiningPlayer = PlayerId("player1")
    val gameProcessor = actorTestKit.spawn(
      GameProcessor.behavior(startingGame, IO.delay(1), 0, RemoteClock.zero)
    )
    val userProbe = actorTestKit.createTestProbe[Either[Unit, ClockedResponse]]()

    "authenticate with an id and a public key" in {
      gameProcessor ! GameProcessor.RunCommand(
        userProbe.ref,
        JoinGame(JoiningPlayer(joiningPlayer, keyPair1.getPublic.asInstanceOf[RSAPublicKey])),
        RemoteClock.of(Map("player1" -> 1))
      )

      userProbe.expectMessageType[Right[Unit, ClockedResponse]].value.event mustBe PlayerJoined(joiningPlayer)
    }

    "a player with the same id but different public key is not accepted" in {
      gameProcessor ! GameProcessor.RunCommand(
        userProbe.ref,
        JoinGame(JoiningPlayer(joiningPlayer, otherKey.getPublic.asInstanceOf[RSAPublicKey])),
        RemoteClock.of(Map("player1" -> 1))
      )

      userProbe.expectMessageType[Right[Unit, ClockedResponse]].value.event mustBe InvalidAction(joiningPlayer)
    }

    "a player claiming the same public key is accepted" in {
      gameProcessor ! GameProcessor.RunCommand(
        userProbe.ref,
        JoinGame(JoiningPlayer(joiningPlayer, keyPair1.getPublic.asInstanceOf[RSAPublicKey])),
        RemoteClock.of(Map("player1" -> 1))
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