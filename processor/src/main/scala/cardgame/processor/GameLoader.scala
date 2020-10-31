package cardgame.processor

import java.io.File
import java.net.URI
import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cardgame.model._
import cardgame.processor.GameProcessor.AdminCommand
import cardgame.processor.config.json._
import cats.effect.{IO, Resource}
import cats.implicits._
import io.circe.Decoder
import io.circe.parser._

import scala.io.Source
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
          game ! AdminCommand(StartGame(deck))
          replyTo ! Right(())
          Behaviors.stopped
      }
  }

  def loadDeck(deckId: DeckId, server: String): Deck = {
    val file = new File(s"decks/${deckId.value.toString}")
    println(s"trying to read from ${file.getAbsolutePath}")
    val allImageFiles = file.listFiles().filter(_.getName.endsWith(".jpg"))
    val configurationFile = new File(s"decks/${deckId.value.toString}/deck.json")
    val (cardConfiguration, startingRules, pointRules) = Resource.fromAutoCloseable(
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
            pointRules = configuration.downField("pointRules").as[PointCounting].toOption
          } yield (cards, startingRules, pointRules)
        }
    }.unsafeRunSync()

    createDeck(allImageFiles, cardConfiguration, startingRules, pointRules, deckId, server)

  }

  private def createDeck(
                          files: Array[File],
                          cardConfiguration: Map[String, Int],
                          startingRules: StartingRules,
                          pointRules: Option[PointCounting],
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
      startingRules,
      pointRules
    )
  }



  sealed trait Protocol
  case class DeckReady(deck: Deck) extends Protocol

  implicit val startingRulesDecoder: Decoder[StartingRules] = Decoder.instance {
    hcursor =>
      (
        hcursor.downField("no").as[List[String]].orElse(Right(List.empty)),
        hcursor.downField("exactlyOne").as[List[String]].orElse(Right(List.empty)),
        hcursor.downField("hand").as[Int],
        hcursor.downField("discardAll").as[List[String]].orElse(Right(List.empty))
      ).mapN(
        (no, exactlyOne, hand, discardAll) =>
          StartingRules(no = no, exactlyOne = exactlyOne, hand = hand, discardAll = discardAll)
      )
  }
}
