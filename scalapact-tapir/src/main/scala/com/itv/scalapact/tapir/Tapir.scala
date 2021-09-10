package com.itv.scalapact.tapir

import cats.data.{EitherNel, NonEmptyList}
import cats.implicits.{catsSyntaxOption, catsSyntaxParallelTraverse_}
import cats.instances.list._
import cats.syntax.either._
import com.itv.scalapact.shared.{Interaction, InteractionResponse}
import io.circe.{Json, ParsingFailure, parser}
import sttp.tapir.apispec.SchemaType.SchemaType
import sttp.tapir.apispec.{Reference, ReferenceOr, Schema, SchemaType}
import sttp.tapir.openapi.{Components, OpenAPI, ResponsesCodeKey, ResponsesKey}

import scala.util.matching.Regex
object Tapir {

  sealed trait OpenApiPactError {
    def message: String = ???
  }
  final case class NoReferenceFoundToSchema(reference: Reference) extends OpenApiPactError
  final case class NoSchemaType(schema: Schema)                   extends OpenApiPactError
  final case class NoItemSchema(schema: Schema)                   extends OpenApiPactError
  final case class MismatchedJsonShape(schema: Schema, schemaType: SchemaType, json: Json) extends OpenApiPactError {
    override def message: String =
      s"Open API Schema ${schema.title.getOrElse("[Untitled]")} was a $schemaType., while a ${json.name}, $json was expected by the pact"
  }
  final case class UnexpectedObjectField(fieldName: String) extends OpenApiPactError {
    override def message: String = s"Field $fieldName required in the PACT response was not in the schema"
  }
  final case class NonIntegerNumber(schema: Schema, json: Json) extends OpenApiPactError {
    override def message = s"Open API Schema ${schema.title.getOrElse("[Untitled]")} was an integer while a float $json was expected by the pact"
  }
  final case class MissingResponseCode(interactionResponse: InteractionResponse)

  final case class NoResponsesExistedForMethod(method: String) extends OpenApiPactError
  final case class InvalidJsonResponse(unmatchedSchemas: Map[ResponsesKey, NonEmptyList[OpenApiPactError]]) extends OpenApiPactError
  case object  NoMatchingPath extends OpenApiPactError
  final case class InvalidPactJson(parsingFailure: ParsingFailure) extends OpenApiPactError
  case object SchemaNotFound extends OpenApiPactError

  private def findSchema(schema: ReferenceOr[Schema], components: Components): Either[OpenApiPactError, Schema] =
    schema match {
      case Left(reference) =>
        components.schemas
          .get(reference.$ref.split('/').last)
          .toRight(NoReferenceFoundToSchema(reference))
          .flatMap(findSchema(_, components))
      case Right(schema) => schema.asRight
    }

  private def pathPatternToRegex(pathPattern: String): Regex =
    pathPattern
      .split('/')
      .map(segment => if (segment.startsWith("{") && segment.endsWith("}")) "[^\\/]*" else segment)
      .mkString("\\/")
      .r

  private def mismatch(schemaName: String, schemaType: String, json: Json): String =
    s"$schemaName was a $schemaType, while a ${json.name}, $json was expected by the pact"

  private def verifyJson(
      json: Json,
      referenceOrSchema: ReferenceOr[Schema],
      components: Components
  ): EitherNel[OpenApiPactError, Unit] =
    for {
      schema <- findSchema(referenceOrSchema, components).toEitherNel
      schemaName = schema.title.getOrElse("unnamed")
      typ <- schema.`type`.toRightNel[OpenApiPactError](NoSchemaType(schema))
      _ <- typ match {
        case SchemaType.Boolean => json.asBoolean.toRightNel(MismatchedJsonShape(schema, typ, json))
        case SchemaType.Object =>
          for {
            jsonObject <- json.asObject.toRightNel(MismatchedJsonShape(schema, typ, json))
            _ <- jsonObject.toList.parTraverse_ { case (key, value) =>
              for {
                propertySchemaOrRef <- schema.properties.get(key).toRightNel(UnexpectedObjectField(key))
                validateProperty    <- verifyJson(value, propertySchemaOrRef, components)
              } yield validateProperty
            }
          } yield ()

        case SchemaType.Array =>
          for {
            jsonArray  <- json.asArray.toRightNel(MismatchedJsonShape(schema, typ, json))
            itemSchema <- schema.items.toRightNel(NoItemSchema(schema))
            _          <- jsonArray.parTraverse_(verifyJson(_, itemSchema, components))
          } yield ()
        case SchemaType.Number => json.asNumber.toRightNel(MismatchedJsonShape(schema, typ, json))
        case SchemaType.String => json.asString.toRightNel(MismatchedJsonShape(schema, typ, json))
        case SchemaType.Integer =>
          json.asNumber
            .toRightNel(MismatchedJsonShape(schema, typ, json))
            .flatMap(_.toBigInt.toRightNel(NonIntegerNumber(schema, json)))
      }
    } yield ()

  def verifyPact(interaction: Interaction, openApi: OpenAPI) =
    interaction.response.body match {
      case Some(body)
          if BodyMatching.hasJsonHeader(interaction.response.headers) || BodyMatching.stringIsProbablyJson(body) =>
        val matchingPath = openApi.paths
          .find { case (path, _) =>
            val regex = pathPatternToRegex(path)
            interaction.request.path match {
              case Some(regex()) => true
              case _             => false
            }
          }
        (for {
          pathItem <- matchingPath.toRightNel[OpenApiPactError](NoMatchingPath).map(_._2)
          method        <- interaction.request.method.toRightNel[OpenApiPactError](NoMatchingPath)
          operation <- (method match {
            case "GET"    => pathItem.get
            case "POST"   => pathItem.post
            case "PUT"    => pathItem.put
            case "PATCH"  => pathItem.patch
            case "DELETE" => pathItem.delete
            case _ => None
          }).toRightNel[OpenApiPactError](NoMatchingPath)
          parsedBody <- parser.parse(body).leftMap[OpenApiPactError](InvalidPactJson).toEitherNel
          components <- openApi.components.toRightNel[OpenApiPactError](SchemaNotFound)
          pactResponseCode <- interaction.response.status.toRightNel[OpenApiPactError](MissingResponseCode(interaction.response))
        } yield
          operation.responses.get(ResponsesCodeKey(pactResponseCode))
          .collect { case Right(response) => response }
          .flatMap(_.content.values)
          .flatMap(_.schema.toList)
          .map(verifyJson(parsedBody, _, components))
      case None => false
    }

}
