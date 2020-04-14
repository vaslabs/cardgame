package cardgame.engine

import cardgame.model._
import cats.effect.IO

import scala.util.Random

object StartedGameOps {

  implicit final class _StartedGameOps(val game: StartedGame) {

    private def draw(playerId: PlayerId): (Game, Event) = {
      val player = game.players.find(_.id == playerId).get
      val draw = game.deck.cards.take(1)
      val newDeck = Deck(game.deck.cards.drop(1))
      val newHand = player.copy(hand = player.hand ++ draw)
      game.copy(players = game.players.updated(game.nextPlayer, newHand), deck = newDeck) ->
        draw.headOption.map(c => GotCard(playerId, c))
          .getOrElse(InvalidAction(playerId))
    }

    private def shuffle(playerId: PlayerId): (Game, Event) =
      if (game.deck.borrowed.isEmpty) {
        val newDeck = Deck(Random.shuffle(game.deck.cards))
        game.copy(deck = newDeck) -> DeckShuffled(newDeck)
      } else
        game -> InvalidAction(playerId)


    private def bottomDraw(playerId: PlayerId): (Game, Event) = {
      val player = game.players.find(_.id == playerId).get
      val draw = game.deck.cards.takeRight(1)
      val newDeck = Deck(game.deck.cards.dropRight(1))
      val newHand = player.copy(hand = player.hand ++ draw)

      game.copy(
        players = game.players.updated(game.nextPlayer, newHand),
        deck = newDeck
      ) ->
        draw.headOption.map(c => GotCard(playerId, c)).getOrElse(InvalidAction(playerId))
    }

    private def chooseNextPlayer(playerId: PlayerId, next: PlayerId): (Game, Event) =
      game.players.indexWhere(_.id == next) match {
        case n if n >= 0 =>
          game.copy(nextPlayer = n) -> NextPlayer(next)
        case _ =>
          game -> InvalidAction(playerId)
      }

    private def endTurn(): (Game, Event) = {
      val nextPlayer = shift(game.players.size, game.nextPlayer, game.direction)
      game.copy(nextPlayer = nextPlayer) -> NextPlayer(game.players(nextPlayer).id)
    }

    private def shift(numberOfPlayers: Int, currentPlayerIndex: Int, direction: Direction): Int =
      direction match {
        case Clockwise => shiftRight(numberOfPlayers, currentPlayerIndex)
        case AntiClockwise => shiftLeft(numberOfPlayers, currentPlayerIndex)
      }


    private def leave(playerId: PlayerId): (Game, Event) = game match {
      case sg@StartedGame(players, _, currentPlayer, direction, _, _) =>
        val killingPlayer = players(currentPlayer)
        if (players.size > 1 && killingPlayer.hand.isEmpty) {
          val remainingPlayers = players.filterNot(_.id == playerId)
          val nextCurrentPlayer = if (currentPlayer > remainingPlayers.size - 1) {
            direction match {
              case Clockwise =>
                0
              case AntiClockwise =>
                remainingPlayers.size - 1
            }
          } else if (currentPlayer == 0) {
            direction match {
              case Clockwise =>
                0
              case AntiClockwise =>
                remainingPlayers.size - 1
            }
          } else {
            currentPlayer
          }
          sg.copy(players = remainingPlayers, nextPlayer = nextCurrentPlayer) -> PlayerLeft(playerId, nextCurrentPlayer)
        } else {
          sg -> InvalidAction(playerId)
        }
    }

    private def play(playerId: PlayerId, cardId: CardId): (Game, Event) = game match {
      case sg@StartedGame(players, _, currentPlayer, _, _, discardPile) =>
        val player = players.find(_.id == playerId).get
        val visibleCardOpt: Option[VisibleCard] = player.hand.find(_.id == cardId).map {
          card => VisibleCard(cardId, card.image)
        }
        val event = visibleCardOpt.map(PlayedCard(_, playerId))
          .getOrElse(InvalidAction(playerId))

        sg.copy(
          players.updated(
            currentPlayer,
            player.copy(hand = player.hand.filterNot(_.id == cardId))
          ),
          discardPile = discardPile.copy(cards = discardPile.cards ++ visibleCardOpt.toList)
        ) -> event
    }


    private def reverse(): (Game, Event) =
      game.copy(direction = game.direction.reverse) -> NewDirection(game.direction.reverse)


    private def borrow(player: PlayerId, index: Int): (Game, Event) = {
        if (index < game.deck.cards.size && index >= 0) {
          val (borrowDeck, card) = game.deck.borrow(index, player)
          card.map(c => game.copy(deck = borrowDeck) -> BorrowedCard(c, player) )
            .getOrElse(game -> InvalidAction(player))
        } else {
          game -> InvalidAction(player)
        }
    }

    private def returnCard(player: PlayerId, card: CardId): (Game, Event) = {
      val borrowedDeck = game.deck.returnCard(card)
      borrowedDeck.map {
        d => game.copy(deck = d) -> ReturnedCard(card, 0)
      }.getOrElse(game -> InvalidAction(player))
    }


    def putCardBack(playerId: PlayerId, card: Card, index: Int): (Game, Event) = game match {
      case sg @ StartedGame(players, deck, currentPlayer, _, _, _) =>
        ifHasTurn(players, currentPlayer, playerId,
          {
            val player = players(currentPlayer)
            val playerCard = player.hand.find(_.id == card.id)
            val playerAfter = player.copy(hand = player.hand.filterNot(_.id == card.id))
            val newDeck = playerCard.map {
              _ => deck.putBack(card, index)
            }
            newDeck.map(d =>
              sg.copy(
                deck = d, players = players.updated(currentPlayer, playerAfter)
              ) -> BackToDeck(card, index)
            ).getOrElse(sg -> InvalidAction(playerId))
          },
          sg
        )
    }

    def steal(player: PlayerId, from: PlayerId, cardIndex: Int): (Game, Event) = game match {
      case sg@StartedGame(players, _, currentPlayer, _, _, _) =>
        ifHasTurn(players, currentPlayer, player,
          {
            val playerTo = players.indexWhere(_.id == player)
            val playerFrom = players.indexWhere(_.id == from)
            val moveCardAction = (playerTo, playerFrom) match {
              case (to, from) if to >= 0 && from >= 0 =>
                players(from).hand.lift(cardIndex).map(_ => cardIndex -> players(playerTo) -> players(playerFrom))
              case _ =>
                None
            }

            moveCardAction match {
              case Some(((card, destination), source)) =>
                val newDest = destination.copy(hand = destination.hand :+ source.hand(card))
                val newSource = source.copy(hand = source.hand.patch(card, Nil, 1))
                sg.copy(
                  players = players
                    .updated(playerTo, newDest)
                    .updated(playerFrom, newSource)
                ) -> MoveCard(source.hand(card), from, player)
              case None =>
                sg -> InvalidAction(player)
            }

          },
          sg
        )
    }

    def recoverCard(playerId: PlayerId, cardId: CardId): (Game, Event) = ifHasTurn(
      game.players, game.nextPlayer, playerId, {
        game.discardPile.cards.find(_.id == cardId).map(
          c => HiddenCard(c.id, c.image)
        ).map { card =>
          val discardPileCards = game.discardPile.cards.filterNot(_.id == cardId)
          val player = game.players(game.nextPlayer)

          val giveCardToPlayer = player.copy(hand = player.hand :+ card)

          game.copy(
            players = game.players.updated(game.nextPlayer, giveCardToPlayer),
            discardPile = DiscardPile(discardPileCards)
          ) -> CardRecovered(playerId, card)
        }.getOrElse(game -> InvalidAction(playerId))
      },
      game
    )

    private def throwDice(playerId: PlayerId, howMany: Int, sides: Int, randomizer: IO[Int]): (Game, Event) = game match {
      case sg @ StartedGame(players, _, current, _, _, _) =>
        ifHasTurn(players, current, playerId, {
          val dice = (1 to howMany).map {
            _ => Math.abs(randomizer.unsafeRunSync())
          }.map(v => Die(sides, v % sides + 1))
          sg -> DiceThrow(playerId, dice.toList)
        }, sg
        )
    }

    private def shuffleHand(playerId: PlayerId): (Game, Event) = {
      val player = game.players.find(_.id == playerId)
      player.map {
        p =>
          p.copy(hand = Random.shuffle(p.hand))
      }.map {
        p =>
          val plIndex = game.players.indexWhere(_.id == p.id)
          game.copy(players = game.players.updated(plIndex, p)) -> ShuffledHand(playerId, p.hand)
      }.getOrElse(game -> InvalidAction(playerId))
    }

    def playingAction(playingGameAction: PlayingGameAction, randomizer: IO[Int]): (Game, Event) = {
      playingGameAction match {
        case a: MustHaveTurnAction =>
          ifHasTurn(
            game.players,
            game.nextPlayer,
            playingGameAction.player,
            a match {
              case DrawCard(playerId) =>
                draw(playerId)
              case PlayCard(cardId: CardId, playerId) =>
                play(playerId, cardId)
              case EndTurn(_) =>
                endTurn()
              case Leave(playerId) =>
                leave(playerId)
              case SwitchDirection(_) =>
                reverse()
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
                this.recoverCard(player, cardId)
              case ThrowDice(p, howMany, sides) =>
                throwDice(p, howMany, sides, randomizer)

              case _: GiveCard =>
                game -> InvalidAction(playingGameAction.player)
            },
            game
          )
        case fa: ShuffleHand =>
          shuffleHand(fa.player)
      }
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
