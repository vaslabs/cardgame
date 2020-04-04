package demo

import sttp.tapir.docs.openapi._
import demo.endpoints.demo._
import sttp.tapir.openapi.circe.yaml._

object Documentation {

  val openApi = List(http, addition).toOpenAPI("Demo addition", "1.0")

  val openApiYaml = openApi.toYaml

}
