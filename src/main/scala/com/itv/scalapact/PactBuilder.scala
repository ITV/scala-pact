package com.itv.scalapact

import com.itv.scalapact.ScalaPactMethods.ScalaPactMethod

object PactBuilder {
  def consumer(name: String): DescribesParticalPact = DescribesParticalPact(name)
}

case class DescribesParticalPact(consumer: String) {
  def hasPactWith(provider: String): DescribesPactBetween = DescribesPactBetween(consumer, provider)
}

case class DescribesPactBetween(consumer: String, provider: String) {

  private var _interactions: List[PactInteraction] = Nil

  def interactions = _interactions

  def withInteraction(interaction: PactInteraction): DescribesPactBetween = {
    _interactions = _interactions ++ List(interaction)
    this
  }

  def withConsumerTest(test: ScalaPactMockConfig => Unit): DescribesPactBetween = {
    ScalaPactMock.runConsumerIntegrationTest(this)(test)
    this
  }

  def writePactContracts(): DescribesPactBetween = {
    ScalaPactContractWriter.writePactContracts(this)
    this
  }
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


