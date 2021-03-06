package cardgame.endpoints.schema

import cardgame.endpoints.schema
import cardgame.model.{Action, ClockedAction, ClockedResponse, Event, PlayerId}
import sttp.tapir.SchemaType.SObjectInfo
import sttp.tapir.{Schema, SchemaType, Validator}

object vector_clock {
  import schema.java_types._

  implicit val actionSchema: Schema[Action] = Schema.derivedSchema[Action]
  implicit val eventSchema: Schema[Event] = Schema.derivedSchema[Event]
  implicit val mapSchema: Schema[Map[String, Long]] = Schema.schemaForMap

  implicit val clockActionSchema: Schema[ClockedAction] = Schema(SchemaType.SCoproduct(
    SObjectInfo("Clocked Action"), List(mapSchema, actionSchema), None
  ))

  implicit val clockResponseSchema: Schema[ClockedResponse] = Schema(SchemaType.SCoproduct(
    SObjectInfo("Clocked Response"), List(mapSchema, eventSchema), None
  ))

  implicit val clockedResponseValidation: Validator[ClockedResponse] = Validator.pass

  implicit lazy val clockActionValidator: Validator[ClockedAction] = Validator.pass

  implicit lazy val playerIdSchema: Schema[PlayerId] =  Schema(SchemaType.SString)
}
