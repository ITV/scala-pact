package com.itv.scalapact.http4s.http4s21.impl

import cats.effect.{IO, Resource}
import com.itv.scalapact.http4s21.impl.ResultPublisher
import com.itv.scalapact.shared._
import org.http4s.client.Client
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.mutable.ArrayBuffer

class ResultPublisherSpec extends FunSpec with Matchers with BeforeAndAfter {

  private val simpleInteraction = Interaction(
    provider_state = None,
    providerState = None,
    description = "a simple request",
    request = InteractionRequest(
      method = Option("GET"),
      path = Option("/"),
      query = None,
      headers = None,
      body = None,
      matchingRules = None
    ),
    response = InteractionResponse(
      status = Option(200),
      headers = None,
      body = Option("""Hello"""),
      None
    )
  )
  private val simple = Pact(
    provider = PactActor("provider"),
    consumer = PactActor("consumer"),
    interactions = List(simpleInteraction),
    _links = None,
    metadata = None
  )

  private val publishUrl = "http://localhost/pacts/provider/provider-service/consumer/consumer-service/latest/{tag}"

  private val _links = Map(
    "pb:publish-verification-results" -> LinkValues(
      title = Option("Publish result url"),
      name = None,
      href = publishUrl,
      templated = Option(true)
    )
  )

  private val simpleWithLinks = simple.copy(_links = Option(_links))

  private var requests: ArrayBuffer[SimpleRequest] = _

  private val fakeCaller: (SimpleRequest, Resource[IO, Client[IO]]) => IO[SimpleResponse] = (req, _) => {
    requests += req
    IO.pure(SimpleResponse(200))
  }

  private val resultPublisher = new ResultPublisher(fakeCaller)

  before {
    requests = ArrayBuffer.empty[SimpleRequest]
  }

  describe("resultPublisher") {
    val brokerPublishData           = BrokerPublishData("1.0.0", Option("http://buildUrl.com"))
    val brokerPublishDataNoBuildUrl = BrokerPublishData("1.0.0", None)
    val successfulResult            = PactVerifyResultInContext(Right(simpleInteraction), "context")
    val successfulResults           = List(successfulResult)
    val failedResult                = PactVerifyResultInContext(Left("failed"), "context")
    val failedResults               = List(successfulResult, failedResult)
    val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, successfulResults))

    it("should publish successful results") {
      val results           = successfulResults
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, results))

      resultPublisher.publishResults(pactVerifyResults, brokerPublishData, None)

      val successfulRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8"),
        Option("""{ "success": "true", "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""),
        None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }

    it("should publish successful results without buildUrl") {
      val results           = successfulResults
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, results))

      resultPublisher.publishResults(pactVerifyResults, brokerPublishDataNoBuildUrl, None)

      val successfulRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8"),
        Option("""{ "success": "true", "providerApplicationVersion": "1.0.0" }"""),
        None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }

    it("should publish failure results") {
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, failedResults))

      resultPublisher.publishResults(pactVerifyResults, brokerPublishData, None)

      val failedRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8"),
        Option("""{ "success": "false", "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""),
        None
      )
      requests shouldBe ArrayBuffer(failedRequest)
    }

    it("should not publish if no _links available") {
      val results           = successfulResults
      val pactVerifyResults = List(PactVerifyResult(simple, results))

      resultPublisher.publishResults(pactVerifyResults, brokerPublishData, None)

      requests shouldBe ArrayBuffer.empty[SimpleRequest]
    }

    it("should add basic auth header if credentials is specified") {
      val results = successfulResults
      val expectedHeader = ("Authorization" -> "Basic dXNlcm5hbWU6cGFzc3dvcmQ=")
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, results))

      resultPublisher.publishResults(pactVerifyResults, brokerPublishData, PactBrokerAuthorization(("username", "password"), ""))

      val successfulRequest = SimpleRequest(
        publishUrl, "", HttpMethod.POST, Map("Content-Type" -> "application/json; charset=UTF-8") + expectedHeader, Option("""{ "success": "true", "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""), None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }

    it("should add bearer token header if a token is specified") {
      val token = "fakeToken"
      val expectedHeader = ("Authorization" -> s"Bearer $token")

      resultPublisher.publishResults(pactVerifyResults, brokerPublishData, PactBrokerAuthorization(("", ""), token))

      val successfulRequest = SimpleRequest(
        publishUrl, "", HttpMethod.POST, Map("Content-Type" -> "application/json; charset=UTF-8") + expectedHeader, Option("""{ "success": "true", "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""), None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }

    it("should have no Authorization header when no auth config is provided") {
      resultPublisher.publishResults(pactVerifyResults, brokerPublishData, None)

      val successfulRequest = SimpleRequest(
        publishUrl, "", HttpMethod.POST, Map("Content-Type" -> "application/json; charset=UTF-8"), Option("""{ "success": "true", "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""), None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }
  }
}
