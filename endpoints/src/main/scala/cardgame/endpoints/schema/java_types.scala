package cardgame.endpoints.schema

import java.net.URI

import sttp.tapir.{Schema, SchemaType}

object java_types {
  implicit val schemaForURI = Schema[URI](SchemaType.SString)
}
