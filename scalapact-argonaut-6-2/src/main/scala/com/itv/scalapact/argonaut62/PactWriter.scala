package com.itv.scalapact.argonaut62

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared.typeclasses.IPactWriter
import com.itv.scalapact.shared.{Pact, PactMetaData, VersionMetaData}

class PactWriter extends IPactWriter {

  import PactImplicits._

  def pactToJsonString(pact: Pact, scalaPactVersion: String): String = {

    val interactions: JsonArray =
      pact.interactions
        .map { i =>
          val maybeRequestBody = i.request.body.flatMap { rb =>
            rb.parseOption.orElse(Option(jString(rb)))
          }

          val maybeResponseBody = i.response.body.flatMap { rb =>
            rb.parseOption.orElse(Option(jString(rb)))
          }

          val bodilessInteraction = i
            .copy(
              request = i.request.copy(body = None),
              response = i.response.copy(body = None)
            )
            .asJson

          val withRequestBody = {
            for {
              requestBody  <- maybeRequestBody
              requestField <- bodilessInteraction.cursor.downField("request")
              bodyField    <- requestField.downField("body")
              updated      <- Option(bodyField.set(requestBody))
            } yield updated.undo
          } match {
            case ok @ Some(_) => ok
            case None         => Option(bodilessInteraction) // There wasn't a body, but there was still an interaction.
          }

          val withResponseBody = {
            for {
              responseBody  <- maybeResponseBody
              responseField <- withRequestBody.flatMap(_.cursor.downField("response"))
              bodyField     <- responseField.downField("body")
              updated       <- Option(bodyField.set(responseBody))
            } yield updated.undo
          } match {
            case ok @ Some(_) => ok
            case None         => withRequestBody // There wasn't a body, but there was still an interaction.
          }

          withResponseBody
        }
        .collect { case Some(s) => s }

    val updatedMetaData: Option[PactMetaData] =
      pact.metadata.orElse {
        Option(
          PactMetaData(
            pactSpecification = Option(VersionMetaData("2.0.0")), //TODO: Where to get this value from?
            `scala-pact` = Option(VersionMetaData(scalaPactVersion))
          )
        )
      }

    val pactNoInteractionsAsJson = pact.copy(interactions = Nil, metadata = updatedMetaData).asJson

    val json: Option[Json] = for {
      interactionsField <- pactNoInteractionsAsJson.cursor.downField("interactions")
      updated           <- Option(interactionsField.withFocus(_.withArray(_ => interactions)))
    } yield updated.undo

    // I don't believe you can ever see this exception.
    json
      .getOrElse(
        throw new Exception(
          "Something went really wrong serialising the following pact into json: " + pact.renderAsString
        )
      )
      .pretty(PrettyParams.spaces2.copy(dropNullKeys = true))
  }

}
