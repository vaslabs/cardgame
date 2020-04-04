package cardgame

import cats.effect.IO


object GameOps {
  implicit final class _GameOps(val game: Game) extends AnyVal {

    def join(player: JoiningPlayer): (Game, Event) = {
      game match {
        case StartingGame(playersJoined) =>
          if (playersJoined.contains(player))
            game -> PlayerJoined(player.id)
          else
            StartingGame(playersJoined :+ player) -> PlayerJoined(player.id)
      }
    }

    def start(deck: Deck, randomizer: IO[Int]): (Game, Event) = {
      game match {
        case StartingGame(players) =>
          val random = randomizer.unsafeRunSync()
          val gamePlayers = players.map(j => PlayingPlayer(j.id, List.empty))
          val startingPlayer = random
          val startedGame =
            StartedGame(
              gamePlayers,
              deck,
              startingPlayer,
              Clockwise,
              List.empty,
              DiscardPile.empty
            )
          startedGame -> GameStarted(gamePlayers(startingPlayer).id)
        case _ =>
          game -> InvalidAction
      }

    }

    def end: (Game, Event) =
      game match {
        case StartingGame(players) =>
          ForcedGameEnd(players) -> GameStopped
        case sg: StartedGame if sg.players.size == 1 =>
          val winner = sg.players(0)
          EndedGame(winner) ->(GameFinished(winner.id))
        case sg: StartedGame =>
          ForcedGameEnd(sg.players) -> GameStopped
      }

    def draw(playerId: PlayerId): (Game, Event) = game match {
      case sg @ StartedGame(players, deck, nextPlayer, _, _, _) =>
        if (hasTurn(players, nextPlayer, playerId)) {
          val player = players.find(_.id == playerId).get
          val draw = deck.cards.take(1)
          val newDeck = Deck(deck.cards.drop(1))
          val newHand = player.copy(hand = player.hand ++ draw)
          sg.copy(players = players.updated(nextPlayer, newHand), deck = newDeck) ->
            draw.headOption.map(c => GotCard(playerId, c.id)).getOrElse(InvalidAction)
        } else
          game -> InvalidAction

    }

    def endTurn(playerId: PlayerId): (Game, Event) = game match {
      case sg @ StartedGame(players, _, currentPlayer, direction, _, _) =>
        if (hasTurn(players, currentPlayer, playerId)) {
          val nextPlayer = shift(players.size, currentPlayer, direction)
          sg.copy(nextPlayer = nextPlayer) -> NextPlayer(players(nextPlayer).id)
        } else
          game -> InvalidAction
      case _ =>
        game -> InvalidAction
    }

    private def shift(numberOfPlayers: Int, currentPlayerIndex: Int, direction: Direction): Int =
      direction match {
        case Clockwise => shiftRight(numberOfPlayers, currentPlayerIndex)
        case AntiClockWise => shiftLeft(numberOfPlayers, currentPlayerIndex)
      }

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

    private def hasTurn(players: List[PlayingPlayer], nextPlayer: Int, playerTryingToPlay: PlayerId): Boolean =
      players.zipWithIndex.find(_._1.id == playerTryingToPlay).exists(_._2 == nextPlayer)


    def leave(playerId: PlayerId): (Game, Event) = game match {
      case sg @ StartedGame(players, _, currentPlayer, _, _, _) =>
        if (hasTurn(players, currentPlayer, playerId)) {
          sg.copy(players.filterNot(_.id == playerId)) -> PlayerLeft(playerId)
        } else
          game -> InvalidAction
      case _ =>
        game -> InvalidAction
    }

    def play(playerId: PlayerId, cardId: CardId): (Game, Event) = game match {
      case sg @ StartedGame(players, _, currentPlayer, _, _, _) =>
        if (hasTurn(players, currentPlayer, playerId)) {
          val player = players.find(_.id == playerId).get
          val event = player.hand.find(_.id == cardId).map {
            card => PlayedCard(VisibleCard(cardId, card.image), playerId)
          }.getOrElse(InvalidAction)
          sg.copy(
            players.updated(
              currentPlayer,
              player.copy(hand = player.hand.filterNot(_.id == cardId))
            )
          ) -> event
        } else
          game -> InvalidAction
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
        case _ =>
          game -> InvalidAction
      }
    }
  }
}
