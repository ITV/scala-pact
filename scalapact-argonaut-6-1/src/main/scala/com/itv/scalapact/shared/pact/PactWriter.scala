package com.itv.scalapact.shared.pact

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.shared.{IPactWriter, Pact}

object PactWriter extends IPactWriter {

  import PactImplicits._

  def pactToJsonString(pact: Pact): String = {

    val interactions: JsonArray =
      pact.interactions
        .map { i =>

          val maybeRequestBody = i.request.body.flatMap { rb =>
            rb.parseOption.orElse(Option(jString(rb)))
          }

          val maybeResponseBody = i.response.body.flatMap { rb =>
            rb.parseOption.orElse(Option(jString(rb)))
          }

          val bodilessInteraction = i.copy(
            request = i.request.copy(body = None),
            response = i.response.copy(body = None)
          ).asJson

          val withRequestBody = {
            for {
              requestBody <- maybeRequestBody
              requestField <- bodilessInteraction.cursor.downField("request")
              bodyField <- requestField.downField("body")
              updated <- Option(bodyField.set(requestBody))
            } yield updated.undo
          } match {
            case ok @ Some(s) => ok
            case None => Option(bodilessInteraction) // There wasn't a body, but there was still an interaction.
          }

          val withResponseBody = {
            for {
              responseBody <- maybeResponseBody
              responseField <- withRequestBody.flatMap(_.cursor.downField("response"))
              bodyField <- responseField.downField("body")
              updated <- Option(bodyField.set(responseBody))
            } yield updated.undo
          } match {
            case ok @ Some(s) => ok
            case None => withRequestBody // There wasn't a body, but there was still an interaction.
          }

          withResponseBody
        }.collect { case Some(s) => s }

    val pactNoInteractionsAsJson = pact.copy(interactions = Nil).asJson

    val json = for {
      interactionsField <- pactNoInteractionsAsJson.cursor.downField("interactions")
      updated <- Option(interactionsField.withFocus(_.withArray(p => interactions)))
    } yield updated.undo

    // I don't believe you can ever see this exception.
    json
      .getOrElse(throw new Exception("Something went really wrong serialising the following pact into json: " + pact))
      .pretty(PrettyParams.spaces2.copy(dropNullKeys = true))
  }

}
