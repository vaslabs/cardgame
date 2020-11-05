package cardgame.endpoints.schema

import java.net.URI
import java.security.interfaces.RSAPublicKey

import sttp.tapir.{Schema, SchemaType}

object java_types {
  implicit val schemaForURI = Schema[URI](SchemaType.SString)
  implicit val schemaForRSAPublicKey = Schema[RSAPublicKey](SchemaType.SString)
}
