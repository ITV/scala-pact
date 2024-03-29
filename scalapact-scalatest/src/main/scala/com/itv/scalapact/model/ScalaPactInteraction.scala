package com.itv.scalapact.model

import com.itv.scalapact.shared.http.HttpMethod

class ScalaPactInteraction(
    description: String,
    providerState: Option[String],
    sslContextName: Option[String],
    request: ScalaPactRequest,
    response: ScalaPactResponse
) {
  def provided(state: String): ScalaPactInteraction =
    new ScalaPactInteraction(description, Option(state), sslContextName, request, response)

  @deprecated("use provided - given is a keyword in Scala 3", "4.0.0")
  def `given`(state: String): ScalaPactInteraction = provided(state)

  def withSsl(sslContextName: String): ScalaPactInteraction =
    new ScalaPactInteraction(description, providerState, Some(sslContextName), request, response)

  def uponReceiving(path: String): ScalaPactInteraction =
    uponReceiving(HttpMethod.GET, path, None, Map.empty, None, ScalaPactMatchingRules.empty)

  def uponReceiving(method: HttpMethod, path: String): ScalaPactInteraction =
    uponReceiving(method, path, None, Map.empty, None, ScalaPactMatchingRules.empty)

  def uponReceiving(method: HttpMethod, path: String, query: Option[String]): ScalaPactInteraction =
    uponReceiving(method, path, query, Map.empty, None, ScalaPactMatchingRules.empty)

  def uponReceiving(
      method: HttpMethod,
      path: String,
      query: Option[String],
      headers: Map[String, String]
  ): ScalaPactInteraction =
    uponReceiving(method, path, query, headers, None, ScalaPactMatchingRules.empty)

  def uponReceiving(
      method: HttpMethod,
      path: String,
      query: Option[String],
      headers: Map[String, String],
      body: String
  ): ScalaPactInteraction =
    uponReceiving(method, path, query, headers, Some(body), ScalaPactMatchingRules.empty)

  def uponReceiving(
      method: HttpMethod,
      path: String,
      query: Option[String],
      headers: Map[String, String],
      body: Option[String],
      matchingRules: ScalaPactMatchingRules
  ): ScalaPactInteraction =
    uponReceiving(ScalaPactRequest(method, path, query, headers, body, matchingRules.toOption))

  def uponReceiving(request: ScalaPactRequest): ScalaPactInteraction =
    new ScalaPactInteraction(
      description,
      providerState,
      sslContextName,
      request,
      response
    )

  def willRespondWith(status: Int): ScalaPactInteraction =
    willRespondWith(status, Map.empty, None, ScalaPactMatchingRules.empty)

  def willRespondWith(status: Int, body: String): ScalaPactInteraction =
    willRespondWith(status, Map.empty, Option(body), ScalaPactMatchingRules.empty)

  def willRespondWith(status: Int, headers: Map[String, String], body: String): ScalaPactInteraction =
    willRespondWith(status, headers, Option(body), ScalaPactMatchingRules.empty)

  def willRespondWith(
      status: Int,
      headers: Map[String, String],
      body: Option[String],
      matchingRules: ScalaPactMatchingRules
  ): ScalaPactInteraction =
    willRespondWith(ScalaPactResponse(status, headers, body, matchingRules.toOption))

  def willRespondWith(response: ScalaPactResponse): ScalaPactInteraction =
    new ScalaPactInteraction(
      description,
      providerState,
      sslContextName,
      request,
      response
    )

  private[scalapact] def finalise: ScalaPactInteractionFinal =
    ScalaPactInteractionFinal(description, providerState, sslContextName, request, response)
}

case class ScalaPactInteractionFinal(
    description: String,
    providerState: Option[String],
    sslContextName: Option[String],
    request: ScalaPactRequest,
    response: ScalaPactResponse
)
