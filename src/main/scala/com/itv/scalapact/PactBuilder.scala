package com.itv.scalapact

import com.itv.scalapact.ScalaPactMethods.ScalaPactMethod

case class PactBuilder(pactContext: String, options: Option[ScalaPactOptions] = Option(ScalaPactOptions(writePactFiles = true))) {
  def consumer(name: String): DescribesParticalPact = DescribesParticalPact(pactContext, name, options)
}

case class ScalaPactOptions(writePactFiles: Boolean = true)

case class DescribesParticalPact(pactContext: String, consumer: String, options: Option[ScalaPactOptions]) {
  def hasPactWith(provider: String): DescribesPactBetween = DescribesPactBetween(pactContext, consumer, provider, Nil, options)
}

case class DescribesPactBetween(pactContext: String, consumer: String, provider: String, interactions: List[PactInteraction], options: Option[ScalaPactOptions]) {

  def withInteraction(interaction: PactInteraction): DescribesPactBetween =
    this.copy(interactions = interactions ++ List(interaction))

  def withConsumerTest(test: ScalaPactMockConfig => Unit): Unit =
    ScalaPactMock.runConsumerIntegrationTest(this)(test)
}

case class PactInteraction(description: String, given: Option[String], request: PactRequest, response: PactResponse)


case class PactRequest(path: String, method: ScalaPactMethod, headers: Map[String, String], body: String) {
  def method(method: ScalaPactMethod): PactRequest = this.copy(method = method)
  def headers(headers: Map[String, String]): PactRequest = this.copy(headers = headers)
  def body(body: String): PactRequest = this.copy(body = body)
}

case class PactResponse(status: Int, headers: Map[String, String], body: String) {
  def status(status: Int): PactResponse = this.copy(status = status)
  def body(body: String): PactResponse = this.copy(body = body)
  def headers(headers: Map[String, String]): PactResponse = this.copy(headers = headers)
}

object uponReceivingRequest {
  def path(path: String): PactRequest = PactRequest(path, ScalaPactMethods.GET, Map(), "")
}

object willRespondWith {
  def status(status: Int): PactResponse = PactResponse(status, Map(), "")
}

object ScalaPactMethods {

  sealed trait ScalaPactMethod {
    val method: String
  }
  case object GET extends ScalaPactMethod { val method = "GET" }
  case object PUT extends ScalaPactMethod { val method = "PUT" }
  case object POST extends ScalaPactMethod { val method = "POST" }
  case object DELETE extends ScalaPactMethod { val method = "DELETE" }
}


