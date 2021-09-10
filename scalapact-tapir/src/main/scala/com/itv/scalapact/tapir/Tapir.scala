package com.itv.scalapact.tapir

import cats.implicits.{catsSyntaxOption, catsSyntaxParallelTraverse_, toFunctorOps}
import cats.instances.list._
import cats.instances.option._
import cats.syntax.foldable._
import cats.syntax.either._
import com.itv.scalapact.shared.Interaction
import io.circe.{Json, parser}
import sttp.tapir.apispec.{ReferenceOr, Schema, SchemaType}
import sttp.tapir.openapi.{Components, OpenAPI}
import cats.data.EitherNel

import scala.util.matching.Regex
object Tapir {



  private def findSchema(schema: ReferenceOr[Schema], components: Components): Either[String, Schema] =
    schema match {
      case Left(reference) => components.schemas.get(reference.$ref.split('/').last).toRight(s"no reference $reference").flatMap(findSchema(_, components))
      case Right(schema)   => schema.asRight
    }

  private def pathPatternToRegex(pathPattern: String): Regex =
    pathPattern
      .split('/')
      .map(segment => if (segment.startsWith("{") && segment.endsWith("}")) "[^\\/]*" else segment)
      .mkString("\\/")
      .r


  private def mismatch(schemaName: String, schemaType: String, json: Json): String = s"$schemaName was a $schemaType, while a ${json.name}, $json was expected by the pact"

  private def verifyJson(json: Json, referenceOrSchema: ReferenceOr[Schema], components: Components): EitherNel[String, Unit] = {
    for {
      schema <- findSchema(referenceOrSchema, components).toEitherNel
      schemaName = schema.title.getOrElse("unnamed")
      typ    <- schema.`type`.toRightNel(s"$schemaName had no type")
      _ <- typ match {
        case SchemaType.Boolean => json.asBoolean.toRightNel(mismatch(schemaName, "boolean", json))
        case SchemaType.Object =>
          for {
            jsonObject <- json.asObject.toRightNel(mismatch(schemaName, "object", json))
            _ <- jsonObject.toList.parTraverse_ { case (key, value) =>
              for {
                propertySchemaOrRef <- schema.properties.get(key).toRightNel(s"key $key was not in the schema")
                validateProperty    <- verifyJson(value, propertySchemaOrRef, components)
              } yield validateProperty
            }
          } yield ()

        case SchemaType.Array =>
          for {
            jsonArray  <- json.asArray.toRightNel(mismatch(schemaName, "array", json))
            itemSchema <- schema.items.toRightNel(s"$schemaName had no items field for array")
            _          <- jsonArray.parTraverse_(verifyJson(_, itemSchema, components))
          } yield ()
        case SchemaType.Number  => json.asNumber.toRightNel(mismatch(schemaName, "number", json))
        case SchemaType.String  => json.asString.toRightNel(mismatch(schemaName, "number", json))
        case SchemaType.Integer  => json.asNumber.toRightNel(mismatch(schemaName, "number", json)).flatMap(_.toBigInt.toRightNel(s"$schemaName was an integer while a float $json was expected by the pact"))
      }
    } yield ()
  }

  sealed trait OpenApiPactError
  case class NoResponsesExistedForMethod(method: String) extends OpenApiPactError
  case class NoResponsesCouldProducePact(errors: NonEm)

  def verifyPact(interaction: Interaction, openApi: OpenAPI): EitherNel[(), Unit] =
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
          (_, pathItem) <- matchingPath
          method        <- interaction.request.method
          operation <- method match {
            case "GET"    => pathItem.get
            case "POST"   => pathItem.post
            case "PUT"    => pathItem.put
            case "PATCH"  => pathItem.patch
            case "DELETE" => pathItem.delete
          }
          parsedBody <- parser.parse(body).toOption
          components <- openApi.components
        } yield operation.responses.values
          .collect { case Right(response) => response }
          .flatMap(_.content.values)
          .flatMap(_.schema.toList)
          .exists(verifyJson(parsedBody, _, components).isDefined))
      case None => false
    }

}
