package cardgame

import java.net.URI
import java.util.UUID

import cats.effect.IO
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random

class GameSpec extends AnyWordSpec with Matchers {

  private def joinPlayers() = List(
    JoiningPlayer(PlayerId("123")),
    JoiningPlayer(PlayerId("124"))
  )

  val players = joinPlayers()
  val commands: LazyList[Action] =
    LazyList.from(players.map(JoinGame))

  val randomizer: IO[Int] = IO.pure(0)

  val deck: Deck = Deck(
    List(
      HiddenCard(CardId(UUID.randomUUID()), URI.create("http://localhost:8080/card1")),
      HiddenCard(CardId(UUID.randomUUID()), URI.create("http://localhost:8080/card2")),
      HiddenCard(CardId(UUID.randomUUID()), URI.create("http://localhost:8080/card3"))
    )
  )

  def initialState(commands: LazyList[Action]) = GameState(
    commands, StartingGame(List.empty), randomizer
  )

  "a game" must {

    "accept players" in {

      val progress = initialState(commands :+ EndGame).start

      val expectedEvents = players.map(_.id).map(PlayerJoined) :+ GameStopped

      progress.toList mustBe expectedEvents
    }

    "start with a given deck" in {
      val progress = initialState(
        commands ++ Seq(
          StartGame(deck),
          DrawCard(PlayerId("123")),
          EndTurn(PlayerId("123")),
          DrawCard(PlayerId("124")),
          EndTurn(PlayerId("124")),
          PlayCard( deck.cards(0).id, PlayerId("123")),
          EndTurn(PlayerId("123")),
          DrawCard(PlayerId("124")),
          Leave(PlayerId("124")),
          EndGame
        )
      )

      val expectedEvents = players.map(_.id).map(PlayerJoined) ++ List(
        GameStarted(PlayerId("123")),
        GotCard(PlayerId("123"), deck.cards(0).id),
        NextPlayer(PlayerId("124")),
        GotCard(PlayerId("124"), deck.cards(1).id),
        NextPlayer(PlayerId("123")),
        PlayedCard(VisibleCard(deck.cards(0).id, deck.cards(0).image), PlayerId("123")),
        NextPlayer(PlayerId("124")),
        GotCard(PlayerId("124"), deck.cards(2).id),
        PlayerLeft(PlayerId("124")),
        GameFinished(PlayerId("123"))
      )

      progress.start.toList mustBe expectedEvents
    }

  }


  "players" can {
    val player1Cards = List(aCard, aCard, aCard, aCard)
    val player2Cards = List(aCard, aCard, aCard)
    val player3Cards = List(aCard, aCard)
    val deckCards = List(aCard, aCard, aCard, aCard)
    val player1 = PlayingPlayer(PlayerId("1"), player1Cards)
    val player2 = PlayingPlayer(PlayerId("2"), player2Cards)
    val player3 = PlayingPlayer(PlayerId("3"), player3Cards)
    val players = List(
      player1,
      player2,
      player3
    )
    val game = StartedGame(players, Deck(deckCards), 0, Clockwise, List.empty, DiscardPile.empty)

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
        GotCard( player3.id, deckCards.head.id)
      )

      GameState(commands, game, randomizer).start.toList mustBe events

    }
  }

  private def aCard = HiddenCard(
    CardId(UUID.randomUUID()),
    URI.create(s"http://localhost:8080/card${Random.nextInt(100)}")
  )
}