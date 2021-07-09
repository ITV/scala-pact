package com.itv.scalapact.tapir

import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import cats.instances.list._
import cats.instances.option._
import cats.syntax.foldable._
import com.itv.scalapact.shared.Interaction
import io.circe.{Json, parser}
import sttp.tapir.apispec.{ReferenceOr, Schema, SchemaType}
import sttp.tapir.openapi.{Components, OpenAPI}

import scala.util.matching.Regex
object Tapir {

  private def findSchema(schema: ReferenceOr[Schema], components: Components): Option[Schema] =
    schema match {
      case Left(reference) => components.schemas.get(reference.$ref).flatMap(findSchema(_, components))
      case Right(schema)   => schema.some
    }
  private def pathPatternToRegex(pathPattern: String): Regex =
    pathPattern
      .split('/')
      .map(segment => if (segment.startsWith("{") && segment.endsWith("}")) "[^\\/]*" else segment)
      .mkString("\\/")
      .r

  private def verifyJson(json: Json, referenceOrSchema: ReferenceOr[Schema], components: Components): Option[Unit] =
    for {
      schema <- findSchema(referenceOrSchema, components)
      typ    <- schema.`type`
      _ = typ match {
        case SchemaType.Boolean => json.isBoolean
        case SchemaType.Object =>
          for {
            jsonObject <- json.asObject
            _ <- jsonObject.toList.traverse_ { case (key, value) =>
              for {
                propertySchemaOrRef <- schema.properties.get(key)
                validateProperty    <- verifyJson(value, propertySchemaOrRef, components)
              } yield validateProperty
            }
          } yield ()

        case SchemaType.Array =>
          for {
            jsonArray  <- json.asArray
            itemSchema <- schema.items
            _          <- jsonArray.traverse_(verifyJson(_, itemSchema, components))
          } yield ()
        case SchemaType.Number  => json.asNumber.void
        case SchemaType.String  => json.asString.void
        case SchemaType.Integer => json.asNumber.void
      }
    } yield ()

  def verifyPact(interaction: Interaction, openApi: OpenAPI): Boolean =
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
          .exists(verifyJson(parsedBody, _, components).isDefined)).getOrElse(false)
      case None => false
    }

}
