package cardgame.processor

import java.util.UUID

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import cardgame.model.{ClockedResponse, DeckId, Game, GameId, JoinGame, JoiningPlayer, PlayerId, PlayingGameAction, RemoteClock, StartingGame}
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
          GameProcessor.behavior(StartingGame(List.empty), intRandomizer, 0L, RemoteClock(Map.empty)),
          gameId.toString
        )
        GameId(gameId)
      },
        ()
      )
      Behaviors.same
    case (ctx, GetGameStatus(gameId, playerId, replyTo)) =>
      gameProcessor(ctx, gameId)
        .map(_ ! GameProcessor.Get(playerId, replyTo))
        .getOrElse(replyTo ! Left(()))
      Behaviors.same
    case (ctx, JoinExistingGame(gameId, playerId, remoteClock, replyTo)) =>
      gameProcessor(ctx, gameId).map(
        _ ! GameProcessor.RunCommand(replyTo, JoinGame(JoiningPlayer(playerId)), remoteClock)
      ).getOrElse(replyTo ! Left(()))
      Behaviors.same
    case (ctx, LoadGame(token, gameId, deckId, server, replyTo)) =>
      replyTo ! Either.cond(
        token == authToken,
        {
          gameProcessor(ctx, gameId).map {
            game =>
              ctx.spawn(
                GameLoader.ephemeralBehaviour(deckId,server, game, replyTo),
                s"Loader-${gameId.value.toString}"
              )
              ()
          }.getOrElse(replyTo ! Left(()))
        },
        ()
      )
      Behaviors.same

    case (ctx, DoGameAction(gameId, action, remoteClock, replyTo)) =>
      gameProcessor(ctx, gameId).map(
        _ ! GameProcessor.RunCommand(replyTo, action, remoteClock)
      ).getOrElse(replyTo ! Left(()))
      Behaviors.same
    case (_, Ignore) =>
      Behaviors.same
  }

  private def gameProcessor(actorContext: ActorContext[_], gameId: GameId): Option[ActorRef[GameProcessor.Protocol]] =
    actorContext.child(gameId.value.toString).map(_.unsafeUpcast[GameProcessor.Protocol])

  sealed trait Protocol

  sealed trait AdminControl extends Protocol {
    def authToken: String
  }


  object Ignore extends Protocol

  case class CreateGame(
           replyTo: ActorRef[Either[Unit, GameId]],
           authToken: String,
  ) extends AdminControl

  case class GetGameStatus(
                           gameId: GameId,
                           playerId: PlayerId,
                           replyTo: ActorRef[Either[Unit, Game]]
  ) extends Protocol

  case class JoinExistingGame(
    gameId: GameId,
    playerId: PlayerId,
    remoteClock: RemoteClock,
    replyTo: ActorRef[Either[Unit, ClockedResponse]]
  ) extends Protocol

  case class LoadGame(
     authToken: String,
     gameId: GameId,
     deckId: DeckId,
     server: String,
     replyTo: ActorRef[Either[Unit, Unit]]
  ) extends AdminControl

  case class DoGameAction(
                          id: GameId,
                          action: PlayingGameAction,
                          remoteClock: RemoteClock,
                          replyTo: ActorRef[Either[Unit, ClockedResponse]]
  ) extends Protocol


  object api {

    implicit final class ActiveGamesOps(actorRef: ActorRef[Protocol]) {
      type CreateGameRes = Either[Unit, GameId]
      type GetGameRes = Either[Unit, Game]
      type ActionRes = Either[Unit, ClockedResponse]

      def createGame(authToken: String)(implicit
                                        timeout: Timeout, scheduler: Scheduler): Future[CreateGameRes] =
        actorRef ? (CreateGame(_, authToken))

      def getGame(gameId: GameId, playerId: PlayerId)(implicit
                                  timeout: Timeout, scheduler: Scheduler): Future[GetGameRes] =
        actorRef ? (GetGameStatus(gameId, playerId, _))

      def joinGame(gameId: GameId, playerId: PlayerId, remoteClock: RemoteClock)(implicit
                                                       timeout: Timeout, scheduler: Scheduler): Future[ActionRes] =
        actorRef ? (JoinExistingGame(gameId, playerId, remoteClock, _))

      def startGame(token: String, gameId: GameId, deckId: DeckId, server: String)(implicit
                    timeout: Timeout, scheduler: Scheduler): Future[Either[Unit, Unit]] =
        actorRef ? (LoadGame(token, gameId, deckId, server, _))

      def action(gameId: GameId, action: PlayingGameAction, remoteClock: RemoteClock)
                (implicit timeout: Timeout, scheduler: Scheduler): Future[ActionRes] = {
        actorRef ? (DoGameAction(gameId, action, remoteClock, _))
      }
    }

  }
}
