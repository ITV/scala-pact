package com.itv.scalapactcore

import argonaut._
import Argonaut._

import scalaz.\/


object PactImplicits {
  implicit lazy val PactCodecJson: CodecJson[Pact] = casecodec3(Pact.apply, Pact.unapply)(
    "provider", "consumer", "interactions"
  )

  implicit lazy val PactActorCodecJson: CodecJson[PactActor] = casecodec1(PactActor.apply, PactActor.unapply)(
    "name"
  )

  implicit lazy val InteractionCodecJson: CodecJson[Interaction] = casecodec4(Interaction.apply, Interaction.unapply)(
    "providerState", "description", "request", "response"
  )

  implicit lazy val InteractionRequestCodecJson: CodecJson[InteractionRequest] = casecodec4(InteractionRequest.apply, InteractionRequest.unapply)(
    "method", "path", "headers", "body"
  )

  implicit lazy val InteractionResponseCodecJson: CodecJson[InteractionResponse] = casecodec3(InteractionResponse.apply, InteractionResponse.unapply)(
    "status", "headers", "body"
  )
}

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])
case class PactActor(name: String)
case class Interaction(providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)
case class InteractionRequest(method: Option[String], path: Option[String], headers: Option[Map[String, String]], body: Option[String]) {
  def unapply: Option[(Option[String], Option[String], Option[Map[String, String]], Option[String])] = Some {
    (method, path, headers, body)
  }
}
case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[String])

object ScalaPactReader {

  import PactImplicits._

  val jsonStringToPact: String => \/[String, Pact] = json =>
    json.decodeEither[Pact]

}

object ScalaPactWriter {

  import PactImplicits._

  val pactToJsonString: Pact => String = pact =>
    pact.asJson.pretty(PrettyParams.spaces2.copy(dropNullKeys = true))

}

