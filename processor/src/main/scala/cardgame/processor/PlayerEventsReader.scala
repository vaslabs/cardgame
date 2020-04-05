package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.{Event, PlayerId}

object PlayerEventsReader {

  def behavior(playerId: PlayerId, replyTo: ActorRef[Event]): Behavior[Event] = Behaviors.setup {
    ctx =>
      ctx.system.eventStream ! EventStream.Subscribe(ctx.self)
      readingEvents(playerId, replyTo)
  }

  private def readingEvents(id: PlayerId, replyTo: ActorRef[Event]): Behavior[Event] = Behaviors.receiveMessage {
    case msg =>
      replyTo ! personalise(msg)
      Behaviors.same
  }

  private def personalise(event: Event): Event = event


}
