package cardgame

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.eventstream.EventStream
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSource
import cardgame.json.circe._
import cardgame.model.{Event, GameCompleted}
import io.circe.syntax._

import scala.concurrent.duration._

package object events {

  def eventSource(implicit actorSystem: ActorSystem[_]): Source[Event, NotUsed] = {
    ActorSource.actorRef[Event](
      {
        case _: GameCompleted =>
      },
      PartialFunction.empty,
      128,
      OverflowStrategy.dropBuffer
    ).mapMaterializedValue {
      streamingActor =>
          actorSystem.eventStream ! EventStream.Subscribe(streamingActor)
          NotUsed
    }
  }

  def toSse(source: Source[Event, NotUsed]): Source[ServerSentEvent, NotUsed] =
    source.map(
      _.asJson.noSpaces
    ).map(
      ServerSentEvent(_, "message")
    ).keepAlive(5 seconds, () => ServerSentEvent.heartbeat)

}
