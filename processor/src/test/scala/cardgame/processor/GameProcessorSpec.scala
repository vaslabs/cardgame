package cardgame.processor

import java.net.URI
import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.eventstream.EventStream
import cardgame.model._
import cardgame.processor.GameProcessor.FireAndForgetCommand
import cardgame.processor.PlayerEventsReader.UserResponse
import cats.effect.IO
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random

class GameProcessorSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll{

  val actorTestKit = ActorTestKit("GameProcessorSpec")

  override def afterAll() =
    actorTestKit.shutdownTestKit()

  "a vector clock supporting game" must {
    val startedGame = GameProcessorSpec.game
    val randomizer = IO.pure(Random.nextInt())
    val playerA = PlayerId("a")
    val playerB = PlayerId("b")
    val playerAClock = Map(playerA -> 0L, playerB -> 0L)

    val streamingPlayer = actorTestKit.createTestProbe[PlayerEventsReader.Protocol]("StreamingPlayer")

    actorTestKit.system.eventStream ! EventStream.Subscribe(streamingPlayer.ref)

    val gameProcessor = actorTestKit.spawn(GameProcessor.behavior(startedGame, randomizer, 0L, RemoteClock.zero))

    "update the local clock and the remote clock on serving a player command" in {
      gameProcessor ! FireAndForgetCommand(DrawCard(playerA), RemoteClock(tick(playerAClock, playerA)))

      val streamingClockedResponse = streamingPlayer.expectMessageType[UserResponse].clockedResponse
      streamingClockedResponse.clock mustBe Map(playerA.value -> 1, playerB.value -> 0)
      streamingClockedResponse.serverClock mustBe 2

      val newPlayerAClock = tick(tick(playerAClock, playerA), playerA)

      gameProcessor ! FireAndForgetCommand(EndTurn(playerA), RemoteClock(tick(newPlayerAClock, playerA)))

      val endTurnStreamingClockedResponse = streamingPlayer.expectMessageType[UserResponse].clockedResponse
      endTurnStreamingClockedResponse.clock mustBe Map(playerA.value -> 3, playerB.value -> 0)
      endTurnStreamingClockedResponse.serverClock mustBe 4

    }

    "out of sync commands are not processed" in {
      val gameStateProbe = actorTestKit.createTestProbe[Either[Unit, Game]]("GameState")
      gameProcessor ! GameProcessor.Get(playerA, gameStateProbe.ref)
      val gameState = gameStateProbe.expectMessageType[Right[Unit, Game]].value
      val outdatedClock = Map(playerA.value -> 3L, playerB.value -> 0L)
      gameProcessor ! FireAndForgetCommand(EndTurn(playerA), RemoteClock.of(outdatedClock))
      streamingPlayer.expectMessageType[UserResponse].clockedResponse.event mustBe OutOfSync(playerA)
      gameProcessor ! GameProcessor.Get(playerA, gameStateProbe.ref)
      gameStateProbe.expectMessageType[Right[Unit, Game]].value mustBe gameState
    }

  }

  def tick(playerClock: Map[PlayerId, Long], playerId: PlayerId): Map[PlayerId, Long] =
    playerClock.updatedWith(playerId)(_.map(_+1))


}


object GameProcessorSpec {

  def game = StartedGame(
    List(GameProcessorSpec.emptyHandedPlayer("a"), GameProcessorSpec.emptyHandedPlayer("b")),
    GameProcessorSpec.randomDeck(10),
    0,
    Clockwise,
    List.empty,
    DiscardPile.empty
  )

  def randomDeck(size: Int): Deck = {
    Deck(
      (0 to size).map(n => HiddenCard(CardId(UUID.randomUUID()), localUri(n))).toList,
      None,
      StartingRules(List.empty, List.empty, 0, List.empty)
    )
  }

  def emptyHandedPlayer(id: String) = PlayingPlayer(PlayerId(id), List.empty, NoGathering, 0)

  def localUri(i: Int): URI = URI.create(s"local://card/${i}")
}