package com.itv.scalapactcore

import argonaut._
import Argonaut._

import scalaz._
import Scalaz._


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

  implicit lazy val InteractionRequestCodecJson: CodecJson[InteractionRequest] = casecodec5(InteractionRequest.apply, InteractionRequest.unapply)(
    "method", "path", "query", "headers", "body"
  )

  implicit lazy val InteractionResponseCodecJson: CodecJson[InteractionResponse] = casecodec3(InteractionResponse.apply, InteractionResponse.unapply)(
    "status", "headers", "body"
  )
}

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])
case class PactActor(name: String)
case class Interaction(providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)
case class InteractionRequest(method: Option[String], path: Option[String], query: Option[String], headers: Option[Map[String, String]], body: Option[String]) {
  def unapply: Option[(Option[String], Option[String], Option[String], Option[Map[String, String]], Option[String])] = Some {
    (method, path, query, headers, body)
  }
}
case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[String])

object ScalaPactReader {

  import PactImplicits._

  val jsonStringToPact: String => \/[String, Pact] = json =>
    json.decodeEither[Pact]

  def rubyJsonToPact(json: String): String \/ Pact = {

    // parse to generic json

    // separate pact from body

    // deserialise pact as normal

    // decide if body is json

    // if json, re-serialise

    // should have a string either way now, so add back into the Pact class file.

    "rubbish".left
  }

}

object ScalaPactWriter {

  import PactImplicits._

  val pactToJsonString: Pact => String = pact =>
    pact.asJson.pretty(PrettyParams.spaces2.copy(dropNullKeys = true))

}

object RubyJsonHelper {

  import PactImplicits._

  val extractPactActor: String => String => Option[PactActor] = field => json => {

    val providerLens = jObjectPL >=> jsonObjectPL(field)

    val b = json.parseOption.flatMap(j => providerLens.get(j))

    b.flatMap(p => p.toString.decodeOption[PactActor])

  }

  val extractInteractions: String => Option[List[(Option[Interaction], Option[String], Option[String])]] = json => {

    val interactionsLens = jObjectPL >=> jsonObjectPL("interactions") >=> jArrayPL
    val requestBodyLens = jObjectPL >=> jsonObjectPL("request") >=> jObjectPL >=> jsonObjectPL("body")
    val responseBodyLens = jObjectPL >=> jsonObjectPL("response") >=> jObjectPL >=> jsonObjectPL("body")

    val interactions = json.parseOption.flatMap(j => interactionsLens.get(j))

    interactions.map { is =>
      is.map { i =>
        val minusRequestBody = for {
          aa <- i.cursor.downField("request")
          bb <- aa.downField("body")
          cc <- bb.delete
        } yield cc.undo

        val minusResponseBody = for {
          aa <- minusRequestBody.flatMap(ii => ii.cursor.downField("response"))
          bb <- aa.downField("body")
          cc <- bb.delete
        } yield cc.undo

        (minusResponseBody.flatMap(p => p.toString.decodeOption[Interaction]), requestBodyLens.get(i).map(_.toString), responseBodyLens.get(i).map(_.toString))
      }
    }
  }

  def go(json: String): Unit = {

    val a = json.parseOption

    println("---------")
    println(a)

    val providerLens = jObjectPL >=> jsonObjectPL("provider")

    val b = a.flatMap(j => providerLens.get(j))

    println("---------")
    println(b)

    val interactionsLens = jObjectPL >=> jsonObjectPL("interactions") >=> jArrayPL

    val c = a.flatMap(j => interactionsLens.get(j).map(_.mkString("\n")))

    println("---------")
    println(c)

  }

}

