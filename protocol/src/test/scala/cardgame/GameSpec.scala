package cardgame

import java.net.URI
import java.util.UUID

import cats.effect.IO
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

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

}