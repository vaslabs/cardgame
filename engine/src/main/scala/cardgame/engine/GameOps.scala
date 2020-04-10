package cardgame.engine

import cats.effect.IO
import cardgame.model._
import StartingGameOps._
import StartedGameOps._
object GameOps {

  implicit final class _GameOps(val game: Game) extends AnyVal {

    def join(player: JoiningPlayer): (Game, Event) = {
      game match {
        case sg: StartingGame =>
          sg.join(player)
        case _ =>
          game -> InvalidAction(player.id)
      }
    }

    def start(deck: Deck, randomizer: IO[Int]): (Game, Event) = {
      game match {
        case sg: StartingGame =>
          sg.start(deck, randomizer)
        case _ =>
          game -> InvalidAction()
      }

    }

    def end: (Game, Event) =
      game match {
        case StartingGame(players) =>
          ForcedGameEnd(players) -> GameStopped()
        case sg: StartedGame if sg.players.size == 1 =>
          val winner = sg.players(0)
          EndedGame(winner) ->(GameFinished(winner.id))
        case sg: StartedGame =>
          ForcedGameEnd(sg.players) -> GameStopped()
      }

    def draw(playerId: PlayerId): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.draw(playerId)
      case _ =>
        game -> InvalidAction(playerId)
    }

    def shuffle(playerId: PlayerId): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.shuffle(playerId)
      case _ =>
        game -> InvalidAction()
    }

    def chooseNextPlayer(player: PlayerId, next: PlayerId): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.chooseNextPlayer(player, next)
      case _ =>
        game -> InvalidAction(player)
    }

    def bottomDraw(playerId: PlayerId): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.bottomDraw(playerId)
      case _ =>
        game -> InvalidAction(playerId)
    }

    def endTurn(playerId: PlayerId): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.endTurn(playerId)
      case _ =>
        game -> InvalidAction(playerId)
    }

    def leave(playerId: PlayerId): (Game, Event) = game match {
      case sg: StartedGame =>
          sg.leave(playerId)
      case _ =>
        game -> InvalidAction(playerId)
    }

    def play(playerId: PlayerId, cardId: CardId): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.play(playerId, cardId)
      case _ =>
        game -> InvalidAction(playerId)
    }

    def reverse(playerId: PlayerId): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.reverse(playerId)
      case other =>
        other -> InvalidAction(playerId)
    }

    def borrow(player: PlayerId, index: Int): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.borrow(player, index)
      case other =>
        other -> InvalidAction(player)
    }

    def returnCard(player: PlayerId, card: CardId): (Game, Event) =  game match {
      case sg: StartedGame =>
        sg.returnCard(player, card)
      case _ =>
        game -> InvalidAction(player)
    }

    def steal(player: PlayerId, from: PlayerId, cardIndex: Int): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.steal(player, from, cardIndex)
      case other =>
        other -> InvalidAction(player)
    }

    def putCardBack(id: PlayerId, card: Card, i: Int): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.putCardBack(id, card, i)
      case _ =>
        game -> InvalidAction(id)
    }

    def recover(player: PlayerId, cardId: CardId): (Game, Event) = game match {
      case sg: StartedGame =>
        sg.recoverCard(player, cardId)
      case _ =>
        game -> InvalidAction(player)
    }

    def action(gameAction: Action, randomizer: IO[Int]): (Game, Event) = {
      gameAction match {
        case jg: JoinGame =>
          join(jg.player)
        case EndGame =>
          end
        case StartGame(deck) =>
          start(deck, randomizer)
        case DrawCard(playerId) =>
          draw(playerId)
        case PlayCard(cardId: CardId, playerId) =>
          play( playerId, cardId)
        case EndTurn(playerId) =>
          endTurn(playerId)
        case Leave(playerId) =>
          leave(playerId)
        case SwitchDirection(playerId) =>
          reverse(playerId)
        case BottomDraw(playerId) =>
          bottomDraw(playerId)
        case BorrowCard(player, idx) =>
          borrow(player, idx)
        case ReturnCard(playerId, cardId) =>
          returnCard(playerId, cardId)
        case s: Shuffle =>
          shuffle(s.player)
        case s: StealCard =>
          steal(s.player, s.from, s.cardIndex)
        case ChooseNextPlayer(playerId, next) =>
          chooseNextPlayer(playerId, next)
        case PutCardBack(card, player, index) =>
          putCardBack(player, card, index)
        case RecoverCard(player, cardId) =>
          recover(player, cardId)
        case p: PlayingGameAction =>
          game -> InvalidAction(p.player)
        case _ =>
          game -> InvalidAction()
      }
    }


  }
}