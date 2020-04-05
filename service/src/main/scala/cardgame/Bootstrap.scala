package cardgame

import java.util.UUID

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.stream.Materializer
import cardgame.processor.ActiveGames
import cardgame.routes.Routes

import scala.io.StdIn

object Bootstrap extends App {

  implicit val system = ActorSystem(Guardian.behaviour, "CardGame")



  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return


}

object Guardian {
  def behaviour: Behavior[Protocol] = Behaviors.setup { ctx =>
    val token = UUID.randomUUID().toString
    ctx.log.warn(s"Admin token is ${token}")
    val activeGames: ActorRef[ActiveGames.Protocol] =
      ctx.spawn(ActiveGames.behavior(token), "ActiveGames")

    implicit val materializer = Materializer(ctx)
    implicit val system = ctx.system.toClassic

    val startingGameRoute = new Routes(activeGames)(ctx.system.scheduler)
    val bindingFuture = Http().bindAndHandle(
      startingGameRoute.main, "0.0.0.0", 8080)


    Behaviors.receiveMessage[Protocol](_ => Behaviors.ignore).receiveSignal {
      case (ctx, PostStop) =>
        import ctx.executionContext
        bindingFuture
          .flatMap(_.unbind()) // trigger unbinding from the port
          .onComplete(_ => system.terminate()) // and shutdown when done
        Behaviors.same
    }
  }

  sealed trait Protocol
}
