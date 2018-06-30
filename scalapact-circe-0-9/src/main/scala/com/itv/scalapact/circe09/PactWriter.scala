package com.itv.scalapact.circe09

import com.itv.scalapact.shared.{JsonRepresentation, MessageContentType, Pact}
import com.itv.scalapact.shared.typeclasses.IPactWriter
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._

class PactWriter extends IPactWriter {

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  def pactToJsonString(pact: Pact): String = {

    val messages = pact.messages.toVector.map { m =>
      val content = m.contentType.jsonRepresentation match {
        case JsonRepresentation.AsString =>
          Json.fromString(m.content)
        case JsonRepresentation.AsObject =>
          parse(m.content).fold(_ => Json.obj(), identity) //we produce a blank object here if the encoder fails!
      }

      Json.obj(
        "description" -> Json.fromString(m.description),
        "providerState" -> m.providerState.asJson,
        "contents" -> content,
        "metaData" -> m.meta.asJson
      )
    }


    val interactions: Vector[Json] =
      pact.interactions.toVector
        .map { i =>
          val maybeRequestBody: Option[Json] = i.request.body.flatMap { rb =>
            parse(rb).toOption.orElse(Option(Json.fromString(rb)))
          }

          val maybeResponseBody: Option[Json] = i.response.body.flatMap { rb =>
            parse(rb).toOption.orElse(Option(Json.fromString(rb)))
          }

          val bodilessInteraction: Json = i
            .copy(
              request = i.request.copy(body = None),
              response = i.response.copy(body = None)
            )
            .asJson

          val withRequestBody: Option[Json] = {
            for {
              requestBody <- maybeRequestBody
              bodyField = bodilessInteraction.hcursor.downField("request").downField("body")
              updated <- Option(bodyField.set(requestBody))
            } yield updated.top
          } match {
            case ok @ Some(_) => ok.flatten
            case None         => Option(bodilessInteraction) // There wasn't a body, but there was still an interaction.
          }

          val withResponseBody: Option[Json] = {
            maybeResponseBody.map { responseBody =>
              withRequestBody
                .flatMap { j =>
                  j.hcursor.downField("response").downField("body").set(responseBody).top
                }
            }
          } match {
            case ok @ Some(_) => ok.flatten
            case None         => withRequestBody // There wasn't a body, but there was still an interaction.
          }

          withResponseBody
        }
        .collect { case Some(s) => s }

    val json: Option[Json] = {
      implicit val messageContentTypeEnc: Encoder[MessageContentType] =
        implicitly[Encoder[String]].contramap(_.renderString)

      pact
        .copy(interactions = Nil, messages = Nil)
        .asJson
        .hcursor
        .downField("messages")
        .withFocus(_.withArray(_ => messages.asJson))
        .downField("interactions")
        .withFocus(_.withArray(_ => interactions.asJson))
        .top
    }

    // I don't believe you can ever see this exception.
    json
      .getOrElse(
        throw new Exception(
          "Something went really wrong serialising the following pact into json: " + pact.renderAsString
        )
      )
      .pretty(Printer.spaces2.copy(dropNullValues = true))
  }

}
