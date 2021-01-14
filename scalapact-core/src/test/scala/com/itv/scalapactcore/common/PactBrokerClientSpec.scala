package com.itv.scalapactcore.common

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.{
  HttpMethod,
  IScalaPactHttpClient,
  IScalaPactHttpClientBuilder,
  SimpleRequest,
  SimpleResponse
}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import java.net.URLEncoder
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

class PactBrokerClientSpec extends FunSpec with Matchers with BeforeAndAfter {

  private val simpleInteraction = Interaction(
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

  private val providerUrl = "http://localhost/pacticipants/provider-service"
  def getProviderUrl(providerVersion: String, tag: String): String =
    providerUrl + "/versions/" + providerVersion + "/tags/" + URLEncoder.encode(tag, "UTF-8")

  private val publishUrl = "http://localhost/pacts/provider/provider-service/consumer/consumer-service/latest/{tag}"

  private val _links = Map(
    "pb:provider" -> LinkValues(
      title = Option("Provider url"),
      name = None,
      href = providerUrl,
      templated = Option(true)
    ),
    "pb:publish-verification-results" -> LinkValues(
      title = Option("Publish result url"),
      name = None,
      href = publishUrl,
      templated = Option(true)
    )
  )

  private val simpleWithLinks = simple.copy(_links = Option(_links))

  private var requests: ArrayBuffer[SimpleRequest] = _

  implicit val clientBuilder: IScalaPactHttpClientBuilder = new IScalaPactHttpClientBuilder {
    def build(clientTimeout: Duration, sslContextName: Option[String], maxTotalConnections: Int): IScalaPactHttpClient =
      new IScalaPactHttpClient {
        override def doRequest(simpleRequest: SimpleRequest): Either[Throwable, SimpleResponse] = {
          requests += simpleRequest
          Right(SimpleResponse(200))
        }
        override def doInteractionRequest(url: String, ir: InteractionRequest): Either[Throwable, InteractionResponse] =
          ???
      }
  }
  implicit val reader: IPactReader = null
  implicit val writer: IPactWriter = null

  private val resultPublisher: PactBrokerClient = new PactBrokerClient

  before {
    requests = ArrayBuffer.empty[SimpleRequest]
  }

  describe("pact broker client verification results publishing") {
    val brokerPublishData           = BrokerPublishData("1.0.0", Option("http://buildUrl.com"))
    val brokerPublishDataNoBuildUrl = BrokerPublishData("1.0.0", None)
    val successfulResult            = PactVerifyResultInContext(Right(simpleInteraction), "context")
    val successfulResults           = List(successfulResult)
    val failedResult                = PactVerifyResultInContext(Left("failed"), "context")
    val failedResults               = List(successfulResult, failedResult)
    val pactVerifyResults           = List(PactVerifyResult(simpleWithLinks, successfulResults))
    val providerVersionTags         = List("master")

    it("should publish successful results") {
      val results           = successfulResults
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, results))

      resultPublisher.publishVerificationResults(pactVerifyResults, brokerPublishData, Nil, None, None, None)

      val successfulRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8"),
        Option("""{ "success": true, "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""),
        None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }

    it("should publish successful results with provider version tags") {
      val results           = successfulResults
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, results))

      resultPublisher.publishVerificationResults(
        pactVerifyResults,
        brokerPublishData,
        providerVersionTags,
        pactBrokerAuthorization = None,
        brokerClientTimeout = None,
        sslContextName = None
      )

      val successfulTagRequest = SimpleRequest(
        baseUrl = getProviderUrl(brokerPublishData.providerVersion, providerVersionTags.head),
        endPoint = "",
        method = HttpMethod.PUT,
        headers = Map("Content-Type" -> "application/json; charset=UTF-8"),
        body = None,
        sslContextName = None
      )
      val successfulVerificationRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8"),
        Option("""{ "success": true, "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""),
        None
      )
      requests shouldBe ArrayBuffer(successfulTagRequest, successfulVerificationRequest)
    }

    it("should publish successful results without buildUrl") {
      val results           = successfulResults
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, results))

      resultPublisher.publishVerificationResults(pactVerifyResults, brokerPublishDataNoBuildUrl, Nil, None, None, None)

      val successfulRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8"),
        Option("""{ "success": true, "providerApplicationVersion": "1.0.0" }"""),
        None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }

    it("should publish failure results") {
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, failedResults))

      resultPublisher.publishVerificationResults(pactVerifyResults, brokerPublishData, Nil, None, None, None)

      val failedRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8"),
        Option("""{ "success": false, "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""),
        None
      )
      requests shouldBe ArrayBuffer(failedRequest)
    }

    it("should not publish if no pb:publish-verification-results available in _links") {
      val results = successfulResults
      val pactVerifyResults = List(
        PactVerifyResult(
          simple.copy(_links = Option(_links.filterNot(_._1 == "pb:publish-verification-results"))),
          results
        )
      )

      resultPublisher.publishVerificationResults(pactVerifyResults, brokerPublishData, Nil, None, None, None)

      requests shouldBe ArrayBuffer.empty[SimpleRequest]
    }

    it("should not publish if no pb:provider available in _links") {
      val results = successfulResults
      val pactVerifyResults = List(
        PactVerifyResult(
          simple.copy(_links = Option(_links.filterNot(_._1 == "pb:provider"))),
          results
        )
      )

      resultPublisher.publishVerificationResults(pactVerifyResults, brokerPublishData, Nil, None, None, None)

      requests shouldBe ArrayBuffer.empty[SimpleRequest]
    }

    it("should add basic auth header if credentials is specified") {
      val results           = successfulResults
      val expectedHeader    = "Authorization" -> "Basic dXNlcm5hbWU6cGFzc3dvcmQ="
      val pactVerifyResults = List(PactVerifyResult(simpleWithLinks, results))

      resultPublisher.publishVerificationResults(
        pactVerifyResults,
        brokerPublishData,
        Nil,
        PactBrokerAuthorization(("username", "password"), ""),
        None,
        None
      )

      val successfulRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8") + expectedHeader,
        Option("""{ "success": true, "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""),
        None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }

    it("should add bearer token header if a token is specified") {
      val token          = "fakeToken"
      val expectedHeader = "Authorization" -> s"Bearer $token"

      resultPublisher.publishVerificationResults(
        pactVerifyResults,
        brokerPublishData,
        Nil,
        PactBrokerAuthorization(("", ""), token),
        None,
        None
      )

      val successfulRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8") + expectedHeader,
        Option("""{ "success": true, "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""),
        None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }

    it("should have no Authorization header when no auth config is provided") {
      resultPublisher.publishVerificationResults(pactVerifyResults, brokerPublishData, Nil, None, None, None)

      val successfulRequest = SimpleRequest(
        publishUrl,
        "",
        HttpMethod.POST,
        Map("Content-Type" -> "application/json; charset=UTF-8"),
        Option("""{ "success": true, "providerApplicationVersion": "1.0.0", "buildUrl": "http://buildUrl.com" }"""),
        None
      )
      requests shouldBe ArrayBuffer(successfulRequest)
    }
  }
}
