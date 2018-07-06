package com.itv.scalapact.circe08

import com.itv.scalapact.shared.Pact
import com.itv.scalapact.shared.typeclasses.IPactWriter
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import PactImplicits._

class PactWriter extends IPactWriter {

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  def pactToJsonString(pact: Pact): String = {
    //TODO: Deal with empty interacations/messages
    //TODO: Add the messages
    //
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

    val json: Option[Json] =
      pact
        .copy(interactions = Nil)
        .asJson
        .hcursor
        .downField("interactions")
        .withFocus(_.withArray(_ => interactions.asJson))
        .top
        .map(removeJsonFieldIf(pact.messages.isEmpty)("messages"))
        .map(removeJsonFieldIf(pact.interactions.isEmpty)("interactions"))

    // I don't believe you can ever see this exception.
    json
      .getOrElse(
        throw new Exception(
          "Something went really wrong serialising the following pact into json: " + pact.renderAsString
        )
      )
      .pretty(Printer.spaces2.copy(dropNullKeys = true))
  }

  private def removeJsonFieldIf(remove: Boolean)(field: String)(json: Json): Json =
    Option(json)
      .filter(_ => remove)
      .flatMap(_.asObject)
      .map(_.filterKeys(_ != field))
      .map(_.asJson)
      .getOrElse(json)

}
