package cardgame

import java.nio.charset.StandardCharsets
import java.security.Signature
import java.security.interfaces.RSAPublicKey

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSource
import cardgame.model._
import cardgame.processor.ActiveGames
import org.bouncycastle.util.encoders.Hex

import scala.util.Try

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

  private def verifySignature(plainText: String, userSignature: String, publicKey: RSAPublicKey): Boolean = {
    val testSignature = Try[Boolean] {
      val signature = Signature.getInstance("SHA256withRSA")
      signature.initVerify(publicKey)
      signature.update(plainText.getBytes(StandardCharsets.UTF_8))
      signature.verify(Hex.decode(userSignature))
    }
    testSignature.getOrElse(false)
  }
  import io.circe.syntax._

  def validateSignature(game: Game, action: ClockedAction): Boolean = {
    val plainText = {
      import cardgame.json.circe.responses._
      action.asJson.mapObject(_.filterKeys(key => key != "signature")).noSpaces
    }
    val verify = (game, action.action) match {
      case (sg: StartedGame, a: PlayingGameAction) =>
        sg.players.find(_.id == a.player).exists(p => verifySignature(plainText, action.signature, p.publicKey))
      case (StartingGame(players), Authorise(playerId)) =>
        players.find(_.id == playerId).exists(p => verifySignature(plainText, action.signature, p.publicKey))
      case (StartingGame(_), JoinGame(JoiningPlayer(_ , publicKey))) =>
        verifySignature(plainText, action.signature, publicKey)
      case _ =>
        true
    }
    verify
  }
}
