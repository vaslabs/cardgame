package cardgame.processor

import java.net.URI
import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.eventstream.EventStream.Publish
import cardgame.model.{AuthorisePlayer, CardId, ClockedResponse, GotCard, PlayerId, RemoteClock, Unauthorised, VisibleCard}
import cardgame.processor.PlayerEventsReader.{UpdateStreamer, UserResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PlayerEventsReaderSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  val actorTestKit = ActorTestKit("GameProcessorSpec")

  override def afterAll() =
    actorTestKit.shutdownTestKit()

  "player events reader" must {
    val player = PlayerId("player")
    val streamer = actorTestKit.createTestProbe[ClockedResponse]()
    val newStreamer = actorTestKit.createTestProbe[ClockedResponse]()

    val playerReader = actorTestKit.spawn(PlayerEventsReader.behavior(player, streamer.ref))
    "not forward game messages before it authenticates" in {
      actorTestKit.system.eventStream ! Publish(UserResponse(ClockedResponse(event, RemoteClock.zero, 0L)))
      streamer.expectMessage(ClockedResponse(Unauthorised, RemoteClock.zero, 0L))
    }
    "not forward game message if another player authenticates" in {
      actorTestKit.system.eventStream ! Publish(UserResponse(ClockedResponse(AuthorisePlayer(PlayerId("other")), RemoteClock.zero, 0L)))
      streamer.expectMessage(ClockedResponse(Unauthorised, RemoteClock.zero, 0L))

      actorTestKit.system.eventStream ! Publish(UserResponse(ClockedResponse(event, RemoteClock.zero, 0L)))
      streamer.expectMessage(ClockedResponse(Unauthorised, RemoteClock.zero, 0L))
    }
    "forward the game message after it authenticates" in {
      actorTestKit.system.eventStream ! Publish(UserResponse(ClockedResponse(AuthorisePlayer(player), RemoteClock.zero, 0L)))
      val eventToPush = event
      actorTestKit.system.eventStream ! Publish(UserResponse(ClockedResponse(eventToPush, RemoteClock.zero, 0L)))
      streamer.expectMessage(ClockedResponse(eventToPush, RemoteClock.zero, 0L))
    }
    "not activate new connection" in {
      playerReader ! UpdateStreamer(newStreamer.ref)
      val eventToPush = event
      actorTestKit.system.eventStream ! Publish(UserResponse(ClockedResponse(eventToPush, RemoteClock.zero, 0L)))
      streamer.expectMessage(ClockedResponse(eventToPush, RemoteClock.zero, 0L))
      newStreamer.expectMessage(ClockedResponse(Unauthorised, RemoteClock.zero, 0L))
    }
    "activate a new connection when it's authorised and deactivate the old one" in {
      actorTestKit.system.eventStream ! Publish(UserResponse(ClockedResponse(AuthorisePlayer(player), RemoteClock.zero, 0L)))
      val eventToPush = event
      actorTestKit.system.eventStream ! Publish(UserResponse(ClockedResponse(eventToPush, RemoteClock.zero, 0L)))
      streamer.expectNoMessage()
      newStreamer.expectMessage(ClockedResponse(eventToPush, RemoteClock.zero, 0L))
    }
  }


  private def event = GotCard(PlayerId("aPlayer"), VisibleCard(CardId(UUID.randomUUID()), URI.create("local://dummy")))

}
