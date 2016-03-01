package com.itv.scalapactcore

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])
case class PactActor(name: String)
case class Interaction(providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)
case class InteractionRequest(method: Option[String], path: Option[String], headers: Option[Map[String, String]], body: Option[String])
case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[String])

object ScalaPactReader {

  val jsonStringToPact: String => Pact = json => Pact(PactActor("me"), PactActor("him"), Nil)

}

object ScalaPactWriter {

  val pactToJsonString: Pact => String = pact => ""

}

