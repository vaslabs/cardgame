package cardgame.engine

import cardgame.model._

object StartedGameOps {

  implicit final class _StartedGameOps(val game: StartedGame) {

    def draw(playerId: PlayerId): (Game, Event) = {
      ifHasTurn(game.players, game.nextPlayer, playerId, {
        val player = game.players.find(_.id == playerId).get
        val draw = game.deck.cards.take(1)
        val newDeck = Deck(game.deck.cards.drop(1), List.empty)
        val newHand = player.copy(hand = player.hand ++ draw)
        game.copy(players = game.players.updated(game.nextPlayer, newHand), deck = newDeck) ->
          draw.headOption.map(c => GotCard(playerId, c.id))
            .getOrElse(InvalidAction(playerId))
      },
        game
      )
    }

    def bottomDraw(playerId: PlayerId): (Game, Event) =
        ifHasTurn(game.players, game.nextPlayer, playerId,
          {
            val player = game.players.find(_.id == playerId).get
            val draw = game.deck.cards.takeRight(1)
            val newDeck = Deck(game.deck.cards.dropRight(1), List.empty)
            val newHand = player.copy(hand = player.hand ++ draw)

            game.copy(
              players = game.players.updated(game.nextPlayer, newHand),
              deck = newDeck
            ) ->
              draw.headOption.map(c => GotCard(playerId, c.id)).getOrElse(InvalidAction(playerId))
          },
          game
        )


    def endTurn(playerId: PlayerId): (Game, Event) =
        ifHasTurn(
          game.players, game.nextPlayer, playerId,
          {
            val nextPlayer = shift(game.players.size, game.nextPlayer, game.direction)
            game.copy(nextPlayer = nextPlayer) -> NextPlayer(game.players(nextPlayer).id)
          },
          game
        )

    private def shift(numberOfPlayers: Int, currentPlayerIndex: Int, direction: Direction): Int =
      direction match {
        case Clockwise => shiftRight(numberOfPlayers, currentPlayerIndex)
        case AntiClockwise => shiftLeft(numberOfPlayers, currentPlayerIndex)
      }


    def leave(playerId: PlayerId): (Game, Event) = game match {
      case sg@StartedGame(players, _, currentPlayer, _, _, _) =>
        ifHasTurn(
          players, currentPlayer, playerId,
          sg.copy(players.filterNot(_.id == playerId)) -> PlayerLeft(playerId),
          sg
        )
      case _ =>
        game -> InvalidAction(playerId)
    }

    def play(playerId: PlayerId, cardId: CardId): (Game, Event) = game match {
      case sg@StartedGame(players, _, currentPlayer, _, _, _) =>
        ifHasTurn(
          players, currentPlayer, playerId,
          {
            val player = players.find(_.id == playerId).get
            val event = player.hand.find(_.id == cardId).map {
              card => PlayedCard(VisibleCard(cardId, card.image), playerId)
            }.getOrElse(InvalidAction(playerId))
            sg.copy(
              players.updated(
                currentPlayer,
                player.copy(hand = player.hand.filterNot(_.id == cardId))
              )
            ) -> event
          },
          sg
        )
      case _ =>
        game -> InvalidAction(playerId)
    }

    def reverse(playerId: PlayerId): (Game, Event) = game match {
      case sg@StartedGame(players, _, currentPlayer, direction, _, _) =>
        ifHasTurn(players, currentPlayer, playerId,
          sg.copy(direction = direction.reverse) -> NewDirection(direction.reverse),
          sg
        )
      case other =>
        other -> InvalidAction(playerId)
    }

    def borrow(player: PlayerId): (Game, Event) = game match {
      case sg@StartedGame(players, deck, currentPlayer, _, _, _) =>
        ifHasTurn(players, currentPlayer, player, {
          val borrowDeck = deck.borrow
          borrowDeck.borrowed.lastOption.map {
            c => sg.copy(deck = borrowDeck) -> BorrowedCard(c.id, player)
          }.getOrElse(sg -> InvalidAction(player))
        },
          sg
        )
      case other =>
        other -> InvalidAction(player)
    }

    def returnCard(player: PlayerId, card: CardId): (Game, Event) = game match {
      case sg@StartedGame(players, deck, currentPlayer, _, _, _) =>
        ifHasTurn(players, currentPlayer, player,
          {
            val borrowedDeck = deck.returnCard(card)
            borrowedDeck.map {
              d => sg.copy(deck = d) -> ReturnedCard(card)
            }.getOrElse(sg -> InvalidAction(player))
          },
          sg
        )
      case _ =>
        game -> InvalidAction(player)
    }

    def steal(player: PlayerId, from: PlayerId, cardIndex: Int): (Game, Event) = game match {
      case sg@StartedGame(players, _, currentPlayer, _, _, _) =>
        ifHasTurn(players, currentPlayer, player,
          {
            val playerTo = players.find(_.id == player).get
            val position = players.indexOf(playerTo)
            val playerFrom = players.find(_.id == from)
            val cardToSteal = playerFrom.flatMap {
              p =>
                p.hand.lift(cardIndex)
            }
            val event = cardToSteal.map(MoveCard(_, from, player)).getOrElse(InvalidAction(player))
            val playerWithCard = cardToSteal.map(c => playerTo.copy(hand = playerTo.hand :+ c)).getOrElse(playerTo)
            sg.copy(players = players.updated(position, playerWithCard)) -> event
          },
          sg
        )
      case other =>
        other -> InvalidAction(player)
    }
  }

  private def ifHasTurn(p: List[PlayingPlayer], ind: Int, pId: PlayerId, t: => (Game, Event), defaultGame: Game): (Game, Event) =
    Either.cond(
      hasTurn(p, ind, pId),
      t,
      defaultGame -> InvalidAction(pId)
    ).merge

  private def hasTurn(players: List[PlayingPlayer], nextPlayer: Int, playerTryingToPlay: PlayerId): Boolean =
    players.zipWithIndex.find(_._1.id == playerTryingToPlay).exists(_._2 == nextPlayer)

  private def shiftRight(size: Int, current: Int): Int =
    if (current == size - 1)
      0
    else
      current + 1

  private def shiftLeft(size: Int, current: Int): Int =
    if (current == 0)
      size - 1
    else
      current - 1


}
