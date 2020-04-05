package cardgame.processor

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import cardgame.model.{Event, Game, GameId, JoinGame, JoiningPlayer, PlayerId, StartingGame}
import cats.effect.IO
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Future
import scala.util.Random

object ActiveGames {

  def behavior(authToken: String): Behavior[Protocol] = Behaviors.receive {
    case (ctx, CreateGame(replyTo, token)) =>
      replyTo ! Either.cond(token == authToken, {
        val gameId = UUID.randomUUID()
        val randomizer = new Random(gameId.getMostSignificantBits)
        val intRandomizer = IO.delay(randomizer.nextInt())
        ctx.spawn(
          GameProcessor.behavior(StartingGame(List.empty), intRandomizer),
          gameId.toString
        )
        ()
      },
        ()
      )
      Behaviors.same
    case (ctx, GetGameStatus(gameId, replyTo)) =>
      ctx.child(gameId.value.toString)
        .map(_.unsafeUpcast[GameProcessor.Protocol])
        .map(_ ! GameProcessor.Get(replyTo))
        .getOrElse(replyTo ! Left(()))
      Behaviors.same
    case (ctx, JoinExistingGame(gameId, playerId, replyTo)) =>
      ctx.child(gameId.value.toString).map(
        _.unsafeUpcast[GameProcessor.Protocol]
      ).map(
        _ ! GameProcessor.RunCommand(replyTo, JoinGame(JoiningPlayer(playerId)))
      ).getOrElse(replyTo ! Left(()))
      Behaviors.same
  }

  sealed trait Protocol

  case class CreateGame(
           replyTo: ActorRef[Either[Unit, Unit]],
           authToken: String,
  ) extends Protocol

  case class GetGameStatus(gameId: GameId, replyTo: ActorRef[Either[Unit, Game]]) extends Protocol

  case class JoinExistingGame(
    gameId: GameId,
    playerId: PlayerId,
    replyTo: ActorRef[Either[Unit, Event]]
  ) extends Protocol


  implicit final class ActiveGamesApi(actorRef: ActorRef[Protocol]) {
    type CreateGameRes = Either[Unit, Unit]
    type GetGameRes = Either[Unit, Game]
    def createGame(authToken: String)(implicit
                    timeout: Timeout, scheduler: Scheduler): Future[CreateGameRes] =
      actorRef ? (CreateGame(_, authToken))
    def getGame(gameId: GameId)(implicit
                timeout: Timeout, scheduler: Scheduler): Future[GetGameRes] =
      actorRef ? (GetGameStatus(gameId, _))
  }
}
