package com.itv.scalapactcore.common.pact

import argonaut.Argonaut._
import argonaut._

object Pact {
  implicit lazy val PactCodecJson: CodecJson[Pact] = casecodec3(Pact.apply, Pact.unapply)(
    "provider", "consumer", "interactions"
  )

  implicit lazy val PactActorCodecJson: CodecJson[PactActor] = casecodec1(PactActor.apply, PactActor.unapply)(
    "name"
  )

  implicit lazy val InteractionCodecJson: CodecJson[Interaction] = casecodec5(Interaction.apply, Interaction.unapply)(
    "provider_state", "providerState", "description", "request", "response"
  )

  implicit lazy val InteractionRequestCodecJson: CodecJson[InteractionRequest] = casecodec6(InteractionRequest.apply, InteractionRequest.unapply)(
    "method", "path", "query", "headers", "body", "matchingRules"
  )

  implicit lazy val InteractionResponseCodecJson: CodecJson[InteractionResponse] = casecodec4(InteractionResponse.apply, InteractionResponse.unapply)(
    "status", "headers", "body", "matchingRules"
  )

  implicit lazy val MatchingRuleCodecJson: CodecJson[MatchingRule] = casecodec3(MatchingRule.apply, MatchingRule.unapply)(
    "match", "regex", "min"
  )

}

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])
case class PactActor(name: String)
case class Interaction(provider_state: Option[String], providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)
case class InteractionRequest(method: Option[String], path: Option[String], query: Option[String], headers: Option[Map[String, String]], body: Option[String], matchingRules: Option[Map[String, MatchingRule]]) {
  def unapply: Option[(Option[String], Option[String], Option[String], Option[Map[String, String]], Option[String])] = Some {
    (method, path, query, headers, body)
  }

  def renderAsString: String =
    s"""Request           [${method.getOrElse("<missing method>")}]
       |  path:           [${path.getOrElse("<missing path>")}]
       |  query:          [${query.getOrElse("<missing path>")}]
       |  headers:        [${headers.map(_.toList.map(p => p._1 + "=" + p._2).mkString(",\n                   ")).getOrElse("")}]
       |  matching rules: [${matchingRules.map(_.toList.map(p => p._1 + " -> (" + p._2.renderAsString + ")").mkString(",\n                   ")).getOrElse("")}]
       |  body:
       |${body.getOrElse("[no body]")}
       |
     """.stripMargin

}
case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[String], matchingRules: Option[Map[String, MatchingRule]]) {

  def renderAsString: String =
    s"""Response          [${status.map(_.toString).getOrElse("<missing status>")}]
       |  headers:        [${headers.map(_.toList.map(p => p._1 + "=" + p._2).mkString(",\\n                   \"")).getOrElse("")}]
       |  matching rules: [${matchingRules.map(_.toList.map(p => p._1 + " -> (" + p._2.renderAsString + ")").mkString(",\n                   ")).getOrElse("")}]
       |  body:
       |${body.getOrElse("[no body]")}
       |
     """.stripMargin

}

case class MatchingRule(`match`: Option[String], regex: Option[String], min: Option[Int]) {
  def renderAsString: String = s"Rule type: '${`match`.getOrElse("<missing>")}'  regex: '${regex.getOrElse("n/a")}'  min: '${min.map(_.toString).getOrElse("n/a")}'"
}

