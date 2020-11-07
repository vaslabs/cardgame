package cardgame

import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, KeyPairGenerator}
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import cats.effect.IO
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import cardgame.model._

import scala.util.Random

class GameSpec extends AnyWordSpec with Matchers {
  def generateKeyPair = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(1024)
    kpg.generateKeyPair()
  }
  private val keyPair1: KeyPair = generateKeyPair
  private val keyPair2 = generateKeyPair
  private val keyPair3 = generateKeyPair
  private def joinPlayers() = List(
    JoiningPlayer(PlayerId("123"), keyPair1.getPublic.asInstanceOf[RSAPublicKey]),
    JoiningPlayer(PlayerId("124"), keyPair2.getPublic.asInstanceOf[RSAPublicKey])
  )

  implicit def toRSAPublicKey(keyPair: KeyPair): RSAPublicKey =
    keyPair.getPublic.asInstanceOf[RSAPublicKey]

  val players = joinPlayers()
  val commands: LazyList[Action] =
    LazyList.from(players.map(JoinGame))

  val randomizer: IO[Int] = IO.pure(0)


  def initialState(commands: LazyList[Action]) = GameState(
    commands, StartingGame(List.empty), randomizer
  )

  def visibleCard(card: Card): VisibleCard = VisibleCard(card.id, card.image)
  "a game" must {
    val deck: Deck = Deck(
      List(
        HiddenCard(CardId(UUID.randomUUID()), URI.create("http://localhost:8080/card1")),
        HiddenCard(CardId(UUID.randomUUID()), URI.create("http://localhost:8080/card2")),
        HiddenCard(CardId(UUID.randomUUID()), URI.create("http://localhost:8080/card3"))
      ),
      None,
      StartingRules.empty,
      None
    )

    "accept players" in {

      val progress = initialState(commands :+ EndGame).start

      val expectedEvents = players.map(_.id).map(PlayerJoined) :+ GameStopped()

      progress.toList mustBe expectedEvents
    }

    "start with a given deck" in {
      val firstPlayer = PlayerId("123")
      val unshuffleDeck = Seq(
        BorrowCard(firstPlayer, 0),
        BorrowCard(firstPlayer, 0),
        BorrowCard(firstPlayer, 0),
        ReturnCard(firstPlayer, deck.cards(2).id),
        ReturnCard(firstPlayer, deck.cards(1).id),
        ReturnCard(firstPlayer, deck.cards(0).id)
      )
      val unshuffledDeckEvents = Seq(
        ReturnedCard(deck.cards(2).id, 0),
        ReturnedCard(deck.cards(1).id, 0),
        ReturnedCard(deck.cards(0).id, 0)
      )
      val progress = initialState(
        commands ++ Seq(
          StartGame(deck)) ++ unshuffleDeck ++
          Seq(
            DrawCard(PlayerId("123")),
            EndTurn(PlayerId("123")),
            DrawCard(PlayerId("124")),
            EndTurn(PlayerId("124")),
            PlayCard( deck.cards(0).id, PlayerId("123")),
            EndTurn(PlayerId("123")),
            DrawCard(PlayerId("124")),
            PlayCard(deck.cards(1).id, PlayerId("124")),
            PlayCard(deck.cards(2).id, PlayerId("124")),
            Leave(PlayerId("124")),
            EndGame
        )
      )

      val expectedEvents = players.map(_.id).map(PlayerJoined) ++
        List(GameStarted(PlayerId("123"))) ++
        unshuffledDeckEvents ++
        Seq(
          GotCard(PlayerId("123"), deck.cards(0)),
          NextPlayer(PlayerId("124")),
          GotCard(PlayerId("124"), deck.cards(1)),
          NextPlayer(PlayerId("123")),
          PlayedCard(visibleCard(deck.cards(0)), PlayerId("123")),
          NextPlayer(PlayerId("124")),
          GotCard(PlayerId("124"), deck.cards(2)),
          PlayedCard(visibleCard(deck.cards(1)), PlayerId("124")),
          PlayedCard(visibleCard(deck.cards(2)), PlayerId("124")),
          PlayerLeft(PlayerId("124"), 0),
          GameFinished(PlayerId("123"))
      )

      progress.start.toList.filterNot(_.isInstanceOf[BorrowedCard]) mustBe expectedEvents
    }

  }


  "players" can {
    val player1Cards = List(aCard, aCard, aCard, aCard)
    val player2Cards = List(aCard, aCard, aCard)
    val player3Cards = List(aCard, aCard)
    val deckCards = List(aCard, aCard, aCard, aCard)
    val player1 = PlayingPlayer(PlayerId("1"), player1Cards, NoGathering, 0, keyPair1)
    val player2 = PlayingPlayer(PlayerId("2"), player2Cards, NoGathering, 0, keyPair2)
    val player3 = PlayingPlayer(PlayerId("3"), player3Cards, NoGathering, 0, keyPair3)
    val players = List(
      player1,
      player2,
      player3
    )
    val game = StartedGame(players, Deck(deckCards, None, StartingRules.empty, None), 0, Clockwise, List.empty, DiscardPile.empty)

    "change the direction" in {
      val cardToPlay = player1.hand(1)
      val commands = LazyList(
        PlayCard(cardToPlay.id, player1.id),
        SwitchDirection(player1.id),
        EndTurn(player1.id),
        DrawCard(player3.id)
      )
      val events = List(
        PlayedCard(VisibleCard(cardToPlay.id, cardToPlay.image), player1.id),
        NewDirection(AntiClockwise),
        NextPlayer(player3.id),
        GotCard( player3.id, deckCards.head)
      )

      cardgame.GameState(commands, game, randomizer).start.toList mustBe events
    }

    "draw from the bottom" in {
      val cardToPlayP1 = player1.hand(1)
      val drawCardP1 = deckCards.last
      val drawCardP2 =  deckCards(deckCards.size - 2)
      val cardToPlayP2 = player2.hand.head
      val commands = LazyList(
        PlayCard(cardToPlayP1.id, player1.id),
        BottomDraw(player1.id),
        ChooseNextPlayer(player1.id, player2.id),
        PlayCard(cardToPlayP2.id, player2.id),
        BottomDraw(player2.id),
        EndTurn(player2.id)
      )

      val events = List(
        PlayedCard(VisibleCard(cardToPlayP1.id, cardToPlayP1.image), player1.id),
        GotCard(player1.id, drawCardP1),
        NextPlayer(player2.id),
        PlayedCard(VisibleCard(cardToPlayP2.id, cardToPlayP2.image), player2.id),
        GotCard(player2.id, drawCardP2),
        NextPlayer(player3.id)
      )

      cardgame.GameState(commands, game, randomizer).start.toList mustBe events
    }

    "borrow cards from the deck and put them back in any order" in {
      val cardsToBorrow = List(deckCards.head, deckCards(1))

      val commands = LazyList(
        BorrowCard(player1.id, 0),
        BorrowCard(player1.id, 0),
        ReturnCard(player1.id, cardsToBorrow(0).id),
        ReturnCard(player1.id, cardsToBorrow(1).id)
      )

      val expectedEvents = List(
        BorrowedCard(cardsToBorrow.head, player1.id),
        BorrowedCard(cardsToBorrow(1), player1.id),
        ReturnedCard(cardsToBorrow(0).id, 0),
        ReturnedCard(cardsToBorrow(1).id, 0)
      )

      cardgame.GameState(commands, game, randomizer).start.toList mustBe expectedEvents
    }

    "steal cards from another player" in {
      val stolenCard = player2.hand.last
      val commands = LazyList(
        StealCard(player1.id, player2.id, player2.hand.size - 1),
        PlayCard(stolenCard.id, player1.id)
      )
      val expectedEvents = LazyList(
        MoveCard(stolenCard, player2.id, player1.id),
        PlayedCard(VisibleCard(stolenCard.id, stolenCard.image), player1.id)
      )

      cardgame.GameState(commands, game, randomizer).start.toList mustBe expectedEvents
    }
    "play cards back to deck" in {
      val commands = LazyList(
        PutCardBack(player1.hand.head, player1.id, deckCards.size),
        EndTurn(player1.id),
        BottomDraw(player2.id),
        BottomDraw(player2.id)
      )

      val expectedEvents = LazyList(
        BackToDeck(player1.hand.head, deckCards.size),
        NextPlayer(player2.id),
        GotCard(player2.id, player1.hand.head),
        GotCard(player2.id, deckCards.last)
      )

      cardgame.GameState(commands, game, randomizer).start.toList mustBe
        expectedEvents
    }

    "recover cards from discard pile" in {
      val expectedPlayedCard = player1.hand.head

      val commands = LazyList(
        PlayCard(player1.hand.head.id, player1.id),
        EndTurn(player1.id),
        RecoverCard(player2.id, expectedPlayedCard.id),
        PlayCard(player1.hand.head.id, player2.id)
      )

      val expectedEvents = LazyList(
        PlayedCard(
          VisibleCard(expectedPlayedCard.id, expectedPlayedCard.image), player1.id
        ),
        NextPlayer(player2.id),
        CardRecovered(player2.id, HiddenCard(expectedPlayedCard.id, expectedPlayedCard.image)),
        PlayedCard(
          VisibleCard(expectedPlayedCard.id, expectedPlayedCard.image),
          player2.id
        )
      )

      cardgame.GameState(commands, game, randomizer).start.toList mustBe
        expectedEvents
    }

    "players can request dice throw" in {

      val commands = LazyList(
        ThrowDice(players.head.id, 2, 6)
      )
      val atomicInteger = new AtomicInteger(-1)
      val dieRandomizer = IO {
        atomicInteger.getAndAdd(1) % 6 + 1
      }

      cardgame.GameState(commands, game, dieRandomizer).start.toList mustBe List(
        DiceThrow(players.head.id, List(Die(6, 1), Die(6, 2)))
      )
    }

    "gather cards if game allows it" in  {

      val firstCard = game.deck.cards.head
      val secondCard = game.deck.cards(1)
      def playGame(game: StartedGame, grabAllowed: Event) = {


        val commands = LazyList(
          DrawCard(player1.id),
          DrawCard(player1.id),
          PlayCard(firstCard.id, player1.id),
          PlayCard(secondCard.id, player1.id),
          GrabCards(players.head.id, List(firstCard, secondCard).map(_.id)),
          GrabCards(players.head.id, List(firstCard, secondCard).map(_.id))
        )
        cardgame.GameState(commands, game, randomizer).start.toList mustBe List(
          GotCard(player1.id, firstCard),
          GotCard(player1.id, secondCard),
          PlayedCard(toVisibleCard(firstCard), player1.id),
          PlayedCard(toVisibleCard(secondCard), player1.id),
          grabAllowed,
          InvalidAction(player1.id)
        )
      }

      playGame(game, InvalidAction(player1.id))

      playGame(
        game.copy(players = players.map(_.copy(gatheringPile = HiddenPile(Set.empty)))),
        AddedToPile(player1.id, Set(toVisibleCard(firstCard), toVisibleCard(secondCard)))
      )
    }

    "when game ends, we can reshuffle all the cards back by counting points" in {
      val points = Map(
        deckCards.head.cardName -> 1,
        deckCards(1).cardName -> 1,
        deckCards(2).cardName -> 2,
        deckCards.last.cardName -> 2
      )
      def otherCards = (0 to 10).map(_ => aCard).filterNot(c => points.keySet.contains(c.cardName)).toSet
      val player1OtherCards = otherCards
      val player2OtherCards = otherCards
      lazy val playersWithGatheredCards = List(
        PlayingPlayer(PlayerId("a"), List.empty, HiddenPile(deckCards.take(2).toSet ++ player1OtherCards), 0, keyPair1),
        PlayingPlayer(PlayerId("b"), List.empty, HiddenPile(deckCards.takeRight(2).toSet ++ player2OtherCards), 0, keyPair2)
      )
      val game = StartedGame(
        playersWithGatheredCards,
        Deck(
          List.empty,
          None,
          StartingRules(
            no = List.empty, exactlyOne = List.empty, hand = 1
          ),
          Some(PointCounting(points, MostCards(3)))
        ),
        1,
        Clockwise,
        List.empty,
        DiscardPile.empty
      )

      val player1ExtraPoints = if (player1OtherCards.size > player2OtherCards.size)
        3
      else
        0
      val player2ExtraPoints = if (player2OtherCards.size > player1OtherCards.size)
        3
      else
        0
      val restartEvent = cardgame.GameState(LazyList(RestartGame(PlayerId("b"))), game, IO(Random.nextInt())).start.head.asInstanceOf[GameRestarted]

      restartEvent.startedGame.players.map(_.gatheringPile) mustBe List(HiddenPile(Set.empty), HiddenPile(Set.empty))
      restartEvent.startedGame.players.map(_.hand.size) mustBe List(1, 1)
      val newDeckCards = (deckCards ++ player1OtherCards.toList ++ player2OtherCards.toList)
          .filterNot(restartEvent.startedGame.players.flatMap(_.hand).contains)
      restartEvent.startedGame.deck.cards must contain theSameElementsAs newDeckCards
      restartEvent.startedGame.players.map(_.points) mustBe List(2 + player1ExtraPoints, 4 + player2ExtraPoints)
    }
  }

  def toVisibleCard(card: Card) = VisibleCard(card.id, card.image)

  private def aCard = HiddenCard(
    CardId(UUID.randomUUID()),
    URI.create(s"http://localhost:8080/card${Random.nextInt(100)}.jpg")
  )
}