package cardgame

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSource
import cardgame.json.circe._
import cardgame.model.{ClockedResponse, GameCompleted, PlayerId}
import cardgame.processor.ActiveGames
import io.circe.syntax._

import scala.concurrent.duration._

package object events {

  def eventSource(playerId: PlayerId, activeGame: ActorRef[ActiveGames.Protocol]): Source[ClockedResponse, NotUsed] = {
    ActorSource.actorRef[ClockedResponse](
      {
        case _: GameCompleted =>
      },
      PartialFunction.empty,
      128,
      OverflowStrategy.dropBuffer
    ).mapMaterializedValue {
      streamingActor =>
          activeGame ! ActiveGames.StreamEventsFor(playerId, streamingActor)
          NotUsed
    }
  }



  def toSse(source: Source[ClockedResponse, NotUsed]): Source[ServerSentEvent, NotUsed] =
    source.map(
      _.asJson.noSpaces
    ).map(
      ServerSentEvent(_, "message")
    ).keepAlive(5 seconds, () => ServerSentEvent.heartbeat)

}
