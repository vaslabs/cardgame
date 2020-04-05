package cardgame.processor

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model._
import cardgame.engine.GameOps._
import cats.effect.IO
object GameProcessor {

  def behavior(game: Game, randomizer: IO[Int]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
        case c: Command =>
          val (gameAffected, event) = game.action(c.action, randomizer)
          ctx.system.eventStream ! EventStream.Publish(event)
          c.replyTo ! Right(event)
          behavior(gameAffected, randomizer)
        case Get(replyTo) =>
          replyTo ! Right(game)
          Behaviors.same
      }
  }


  sealed trait Protocol

  sealed trait Command extends Protocol {
    def replyTo: ActorRef[Either[Unit, Event]]
    def action: Action
  }

  case class RunCommand(replyTo: ActorRef[Either[Unit, Event]], action: Action) extends Command

  sealed trait Query extends Protocol {
    def replyTo: ActorRef[Either[Unit, Game]]
  }

  case class Get(replyTo: ActorRef[Either[Unit, Game]]) extends Query

}
