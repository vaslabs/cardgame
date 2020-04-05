package cardgame.routes

import cardgame.endpoints.JoiningGame
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.docs.openapi._

object Documentation {

  val openApi = List(JoiningGame.joinPlayer)
    .toOpenAPI("Demo addition", "1.0")

  val openApiYaml = openApi.toYaml

}
