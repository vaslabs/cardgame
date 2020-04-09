package cardgame.processor

import java.util.UUID

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import cardgame.model.{DeckId, Event, Game, GameId, JoinGame, JoiningPlayer, PlayerId, PlayingGameAction, StartingGame}
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
    case (ctx, JoinExistingGame(gameId, playerId, replyTo)) =>
      gameProcessor(ctx, gameId).map(
        _ ! GameProcessor.RunCommand(replyTo, JoinGame(JoiningPlayer(playerId)))
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

    case (ctx, DoGameAction(gameId, action, replyTo)) =>
      gameProcessor(ctx, gameId).map(
        _ ! GameProcessor.RunCommand(replyTo, action)
      ).getOrElse(replyTo ! Left(()))
      Behaviors.same
  }

  private def gameProcessor(actorContext: ActorContext[_], gameId: GameId): Option[ActorRef[GameProcessor.Protocol]] =
    actorContext.child(gameId.value.toString).map(_.unsafeUpcast[GameProcessor.Protocol])

  sealed trait Protocol

  sealed trait AdminControl extends Protocol {
    def authToken: String
  }

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
    replyTo: ActorRef[Either[Unit, Event]]
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
                          replyTo: ActorRef[Either[Unit, Event]]
  ) extends Protocol


  object api {

    implicit final class ActiveGamesOps(actorRef: ActorRef[Protocol]) {
      type CreateGameRes = Either[Unit, GameId]
      type GetGameRes = Either[Unit, Game]
      type ActionRes = Either[Unit, Event]

      def createGame(authToken: String)(implicit
                                        timeout: Timeout, scheduler: Scheduler): Future[CreateGameRes] =
        actorRef ? (CreateGame(_, authToken))

      def getGame(gameId: GameId, playerId: PlayerId)(implicit
                                  timeout: Timeout, scheduler: Scheduler): Future[GetGameRes] =
        actorRef ? (GetGameStatus(gameId, playerId, _))

      def joinGame(gameId: GameId, playerId: PlayerId)(implicit
                                                       timeout: Timeout, scheduler: Scheduler): Future[ActionRes] =
        actorRef ? (JoinExistingGame(gameId, playerId, _))

      def startGame(token: String, gameId: GameId, deckId: DeckId, server: String)(implicit
                    timeout: Timeout, scheduler: Scheduler): Future[Either[Unit, Unit]] =
        actorRef ? (LoadGame(token, gameId, deckId, server, _))

      def action(gameId: GameId, action: PlayingGameAction)
                (implicit timeout: Timeout, scheduler: Scheduler): Future[ActionRes] = {
        actorRef ? (DoGameAction(gameId, action, _))
      }
    }

  }
}