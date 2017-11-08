package com.itv.scalapact.shared.pact

import com.itv.scalapact.shared.{IPactWriter, Pact}
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._

object PactWriter extends IPactWriter {

  // Used by old Scala versions
  import EitherWithToOption._

  def pactToJsonString(pact: Pact): String = {

    val interactions: Vector[Json] =
      pact.interactions.toVector
        .map { i =>

          val maybeRequestBody = i.request.body.flatMap { rb =>
            parse(rb).toOption.orElse(Option(Json.fromString(rb)))
          }

          val maybeResponseBody = i.response.body.flatMap { rb =>
           parse(rb).toOption.orElse(Option(Json.fromString(rb)))
          }

          val bodilessInteraction = i.copy(
            request = i.request.copy(body = None),
            response = i.response.copy(body = None)
          ).asJson

          val withRequestBody = {
            for {
              requestBody <- maybeRequestBody
              bodyField = bodilessInteraction.hcursor.downField("request").downField("body")
              updated <- Option(bodyField.set(requestBody))
            } yield updated.top
          } match {
            case ok @ Some(_) => ok.flatten
            case None => Option(bodilessInteraction) // There wasn't a body, but there was still an interaction.
          }

          val withResponseBody = {
            maybeResponseBody.map { responseBody =>
              withRequestBody
                .flatMap { j =>
                  j.hcursor.downField("response").downField("body").set(responseBody).top
                }
            }
          } match {
            case ok @ Some(_) => ok.flatten
            case None => withRequestBody // There wasn't a body, but there was still an interaction.
          }

          withResponseBody
        }.collect { case Some(s) => s }

    val json: Option[Json] =
      pact.copy(interactions = Nil)
        .asJson
        .hcursor
        .downField("interactions")
        .withFocus(_.withArray(_ => interactions.asJson))
        .top

    // I don't believe you can ever see this exception.
    json
      .getOrElse(throw new Exception("Something went really wrong serialising the following pact into json: " + pact.renderAsString))
      .pretty(Printer.spaces2.copy(dropNullValues = true))
  }

}
