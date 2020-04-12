package cardgame.processor

import java.io.File
import java.net.URI
import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model.{CardId, Deck, DeckId, HiddenCard, StartGame, StartingRules}
import cardgame.processor.GameProcessor.FireAndForgetCommand
import cats.effect.{IO, Resource}

import scala.io.Source
import io.circe.parser._
import io.circe.generic.auto._

object GameLoader {

  def ephemeralBehaviour(
    deckId: DeckId,
    server: String,
    game: ActorRef[GameProcessor.Protocol],
    replyTo: ActorRef[Either[Unit, Unit]]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      ctx.log.info(s"Loading deck ${deckId}")
      ctx.self ! DeckReady(loadDeck(deckId, server))
      Behaviors.receiveMessage {
        case DeckReady(deck) =>
          game ! FireAndForgetCommand(StartGame(deck))
          replyTo ! Right(())
          Behaviors.stopped
      }
  }

  def loadDeck(deckId: DeckId, server: String): Deck = {
    val file = new File(s"decks/${deckId.value.toString}")
    println(s"trying to read from ${file.getAbsolutePath}")
    val allImageFiles = file.listFiles().filter(_.getName.endsWith(".jpg"))
    val configurationFile = new File(s"decks/${deckId.value.toString}/deck.json")
    val (cardConfiguration, startingRules) = Resource.fromAutoCloseable(
      IO {
        Source.fromFile(configurationFile, "utf-8")
      }
    ).use {
      source =>
        IO.fromEither {
          val json = source.mkString
          for {
            configuration <- parse(json).map(_.hcursor)
            cards <- configuration.downField("cards").as[Map[String, Int]]
            startingRules <- configuration.downField("startingRules").as[StartingRules]
          } yield (cards, startingRules)
        }
    }.unsafeRunSync()

    createDeck(allImageFiles, cardConfiguration, startingRules, deckId, server)

  }

  private def createDeck(
                          files: Array[File],
                          cardConfiguration: Map[String, Int],
                          startingRules: StartingRules,
                          deckId: DeckId, server: String
  ): Deck = {
    val cards = files.flatMap(
      file => {
        val name = file.getName.substring(0, file.getName.size - 4)
        val cards = cardConfiguration.getOrElse(name, 0)
        (1 to cards).map {
          _ =>
            HiddenCard(
              CardId(UUID.randomUUID()),
              URI.create(s"${server}/img/${deckId.value.toString}/${file.getName}")
            )
        }
      }
    ).toList
    Deck(
      cards,
      None,
      startingRules
    )
  }



  sealed trait Protocol
  case class DeckReady(deck: Deck) extends Protocol
}
