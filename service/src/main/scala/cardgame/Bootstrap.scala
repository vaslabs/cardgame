package cardgame

import java.nio.charset.StandardCharsets
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.UUID

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.stream.Materializer
import cardgame.model.{ClockedAction, Game, JoiningGameAction, PlayingGameAction, StartedGame, StartingGame}
import cardgame.processor.ActiveGames
import cardgame.routes.Routes

import scala.io.StdIn

object Bootstrap extends App {

  implicit val system = ActorSystem(Guardian.behaviour, "CardGame")



  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return

  sys.addShutdownHook(system.terminate())

}

object Guardian {
  import cardgame.json.circe._
  def behaviour: Behavior[Protocol] = Behaviors.setup { ctx =>
    val token = UUID.randomUUID().toString
    ctx.log.warn(s"Admin token is ${token}")
    val activeGames: ActorRef[ActiveGames.Protocol] =
      ctx.spawn(ActiveGames.behavior(token)(validateSignature), "ActiveGames")

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


  private def verifySignature(plainText: String, userSignature: String, publicKey: RSAPublicKey): Boolean = {
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initVerify(publicKey)
    signature.update(plainText.getBytes(StandardCharsets.UTF_8))
    signature.verify(userSignature.getBytes(StandardCharsets.UTF_8))
  }
  import io.circe.syntax._
  import cardgame.json.circe._

  private def validateSignature(game: Game, action: ClockedAction): Boolean = {
    val plainText = {
      action.asJson.mapObject(_.filterKeys(key => key != "signature")).noSpaces
    }
    val verify = (game, action.action) match {
      case (sg: StartedGame, a: PlayingGameAction) =>
        sg.players.find(_.id == a.player).exists(p => verifySignature(plainText, action.signature, p.publicKey))
      case (sg: StartingGame, a: JoiningGameAction) =>
        sg.playersJoined.find(_.id == a.playerId).exists(
          p => verifySignature(plainText, action.signature, p.publicKey)
        )
      case _ =>
        true
    }
    println(verify)
    true
  }
}
