package com.itv.scalapact.tapir

import cats.data.{EitherNel, NonEmptyList}
import cats.implicits.{catsSyntaxOption, catsSyntaxParallelTraverse_}
import cats.instances.list._
import cats.syntax.either._
import com.itv.scalapact.shared.{Interaction, InteractionResponse}
import io.circe.{Json, ParsingFailure, parser}
import sttp.tapir.apispec.SchemaType.SchemaType
import sttp.tapir.apispec.{Reference, ReferenceOr, Schema, SchemaType}
import sttp.tapir.openapi.{Components, ResponsesCodeKey, ResponsesKey}
import Resolution._

import scala.util.matching.Regex
object Tapir {

  sealed trait OpenApiPactError {
    def message: String = ???
  }
  final case class NoReferenceFoundToSchema(reference: Reference) extends OpenApiPactError
  final case class NoSchemaType(schema: Schema)                   extends OpenApiPactError
  final case object NoJsonSchema                   extends OpenApiPactError
  final case class NoItemSchema(schema: ResolvedSchema)                   extends OpenApiPactError
  final case class MismatchedJsonShape(schema: ResolvedSchema, schemaType: SchemaType, json: Json) extends OpenApiPactError {
    override def message: String =
      s"Open API Schema ${schema.title.getOrElse("[Untitled]")} was a $schemaType., while a ${json.name}, $json was expected by the pact"
  }
  final case class UnexpectedObjectField(fieldName: String) extends OpenApiPactError {
    override def message: String = s"Field $fieldName required in the PACT response was not in the schema"
  }
  final case class NonIntegerNumber(schema: ResolvedSchema, json: Json) extends OpenApiPactError {
    override def message = s"Open API Schema ${schema.title.getOrElse("[Untitled]")} was an integer while a float $json was expected by the pact"
  }
  final case class MissingResponseCode(interactionResponse: InteractionResponse) extends OpenApiPactError

  final case class NoResponsesExistedForMethod(method: String) extends OpenApiPactError
  final case class NoResponsesExistedForResponseCode(key: ResponsesCodeKey) extends OpenApiPactError
  final case class InvalidJsonResponse(unmatchedSchemas: Map[ResponsesKey, NonEmptyList[OpenApiPactError]]) extends OpenApiPactError
  case object NoMatchingPath extends OpenApiPactError
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


  def verifyJson(json: Json, schema: ResolvedSchema): EitherNel[OpenApiPactError, Unit] = 
    for {
      _ <- schema.`type` match {
        case typ @ SchemaType.Boolean => json.asBoolean.toRightNel(MismatchedJsonShape(schema, typ, json))
        case t@ SchemaType.Object =>
          for {
            jsonObject <- json.asObject.toRightNel(MismatchedJsonShape(schema, t, json))
            _ <- jsonObject.toList.parTraverse_ { case (key, value) =>
              for {
                propertySchemaOrRef <- schema.properties.get(key).toRightNel(UnexpectedObjectField(key))
                validateProperty    <- verifyJson(value, propertySchemaOrRef)
              } yield validateProperty
            }
          } yield ()

        case typ @ SchemaType.Array =>
          for {
            jsonArray  <- json.asArray.toRightNel(MismatchedJsonShape(schema, typ, json))
            itemSchema <- schema.items.toRightNel(NoItemSchema(schema))
            _          <- jsonArray.parTraverse_(verifyJson(_, itemSchema))
          } yield ()
        case typ @ SchemaType.Number => json.asNumber.toRightNel(MismatchedJsonShape(schema, typ, json))
        case typ @ SchemaType.String => json.asString.toRightNel(MismatchedJsonShape(schema, typ, json))
        case typ @ SchemaType.Integer =>
          json.asNumber
            .toRightNel(MismatchedJsonShape(schema, typ, json))
            .flatMap(_.toBigInt.toRightNel(NonIntegerNumber(schema, json)))
      }
    } yield ()

  def verifyPact(interaction: Interaction, openApi: Resolution.ResolvedOpenApi) =
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
          pactResponseCode <- interaction.response.status.toRightNel[OpenApiPactError](MissingResponseCode(interaction.response))
          responseKey = ResponsesCodeKey(pactResponseCode)
          response <- operation.responses.get(responseKey).toRightNel[OpenApiPactError](NoResponsesExistedForResponseCode(responseKey))
          mediaType <- response.content.get("/application/json").toRightNel[OpenApiPactError](NoJsonSchema)
          _ <- verifyJson(parsedBody, mediaType.schema)
        } yield ())
      case None => false
    }

}
