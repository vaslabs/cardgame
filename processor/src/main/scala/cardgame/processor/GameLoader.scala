package cardgame.processor

import java.io.File
import java.net.URI
import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model.{CardId, Deck, DeckId, HiddenCard, StartGame}
import cardgame.processor.GameProcessor.FireAndForgetCommand
import cats.effect.{IO, Resource}

import scala.io.Source
import io.circe.parser._

object GameLoader {

  def ephemeralBehaviour(
    deckId: DeckId,
    game: ActorRef[GameProcessor.Protocol],
    replyTo: ActorRef[Either[Unit, Unit]]): Behavior[Protocol] = Behaviors.setup {
    ctx =>
      ctx.log.info(s"Loading deck ${deckId}")
      ctx.self ! DeckReady(loadDeck(deckId))
      Behaviors.receiveMessage {
        case DeckReady(deck) =>
          game ! FireAndForgetCommand(StartGame(deck))
          replyTo ! Right(())
          Behaviors.stopped
      }
  }

  def loadDeck(deckId: DeckId): Deck = {
    val file = new File(s"decks/${deckId.value.toString}")
    println(s"trying to read from ${file.getAbsolutePath}")
    val allImageFiles = file.listFiles().filter(_.getName.endsWith(".jpg"))
    val configurationFile = new File(s"decks/${deckId.value.toString}/deck.json")
    val deckConfiguration = Resource.fromAutoCloseable(
      IO {
        Source.fromFile(configurationFile, "utf-8")
      }
    ).use {
      source =>
        IO.fromEither {
          val json = source.mkString
          parse(json).flatMap(_.as[Map[String, Int]])
        }
    }.unsafeRunSync()

    createDeck(allImageFiles, deckConfiguration, deckId)

  }

  private def createDeck(files: Array[File], configuration: Map[String, Int],deckId: DeckId): Deck = Deck {
    files.flatMap {
      file =>
        val name = file.getName.substring(0, file.getName.size - 4)
        val cards = configuration.getOrElse(name, 0)
        (0 to cards).map {
          _ =>
            HiddenCard(
              CardId(UUID.randomUUID()),
              URI.create(s"http://localhost:8080/img/${deckId.value.toString}${file.getName}")
            )
        }
    }.toList
  }



  sealed trait Protocol
  case class DeckReady(deck: Deck) extends Protocol
}
