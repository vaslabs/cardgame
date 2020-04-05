package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.{Action, Event, Game}
import cardgame.GameOps._
import cats.effect.IO
object GameProcessor {

  def behavior(game: Game, randomizer: IO[Int]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
        case c: Command =>
          val (gameAffected, event) = game.action(c.action, randomizer)
          ctx.system.eventStream ! EventStream.Publish(event)
          c.replyTo ! event
          behavior(gameAffected, randomizer)
        case Get(replyTo) =>
          replyTo ! game
          Behaviors.same
      }
  }


  sealed trait Protocol

  sealed trait Command extends Protocol {
    def replyTo: ActorRef[Event]
    def action: Action
  }

  sealed trait Query extends Protocol {
    def replyTo: ActorRef[Game]
  }

  case class Get(replyTo: ActorRef[Game]) extends Query

}
