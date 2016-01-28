package com.itv.scalapact

import org.json4s.DefaultFormats
import org.json4s.native.JsonParser._
import org.json4s.native.Serialization._
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.xml.XML
import scalaj.http.{Http, HttpRequest}

class ExampleSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  private implicit val formats = DefaultFormats

  private val pact = PactBuilder.consumer("consumer").hasPactWith("provider")

  override def afterAll() = {
    pact.writePactContracts()
  }

  describe("Example CDC Integration tests") {

    it("Should be able to create a contract for a simple GET") {

      val endPoint = "/hello"

      pact
        .withInteraction(
          PactInteraction(
            description = "Fetch a greeting",
            given = None,
            uponReceivingRequest
              .path(endPoint),
            willRespondWith
              .status(200)
              .body("Hello there!")
          )
        )
        .withConsumerTest { scalaPactMockConfig =>

          val result = SimpleClient.doGetRequest(scalaPactMockConfig.baseUrl, endPoint, Map())

          result.status should equal(200)
          result.body should equal("Hello there!")
        }

    }

    it("Should be able to create a contract for a GET request for JSON data") {

      val data = Person("Joe", 25, "London", List("Fishing", "Karate"))

      val endPoint = "/json"

      pact
        .withInteraction(
          PactInteraction(
            description = "Request for some json",
            given = None,
            uponReceivingRequest
              .path(endPoint),
            willRespondWith
              .status(200)
              .headers(Map("Content-Type" -> "application/json"))
              .body(write(data))
          )
        )
        .withConsumerTest { scalaPactMockConfig =>

          val result = SimpleClient.doGetRequest(scalaPactMockConfig.baseUrl, endPoint, Map())

          withClue("Status mismatch") {
            result.status should equal(200)
          }

          withClue("Headers did not match") {
            result.headers.exists(h => h._1 == "Content-Type" && h._2 == "application/json")
          }

          withClue("Body did not match") {
            (parse(result.body).extract[Person] == data) should equal(true)
          }

        }

    }

    it("Should be able to create a contract for posting json and getting an xml response") {

      val requestData = Person("Joe", 25, "London", List("Fishing", "Karate"))

      val responseXml = <a><b>Error in your json</b></a>

      val endPoint = "/post-json"

      val headers = Map("Content-Type" -> "application/json")

      pact
        .withInteraction(
          PactInteraction(
            description = "Request for some json",
            given = None,
            uponReceivingRequest
              .path(endPoint)
              .method(ScalaPactMethods.POST)
              .headers(headers)
              .body(write(requestData)),
            willRespondWith
              .status(400)
              .headers(Map("Content-Type" -> "text/xml"))
              .body(responseXml.toString())
          )
        )
        .withConsumerTest { scalaPactMockConfig =>

          val result = SimpleClient.doPostRequest(scalaPactMockConfig.baseUrl, endPoint, headers, write(requestData))

          withClue("Status mismatch") {
            result.status should equal(400)
          }

          withClue("Headers did not match") {
            result.headers.exists(h => h._1 == "Content-Type" && h._2 == "text/xml")
          }

          withClue("Response body xml did not match") {
            XML.loadString(result.body) should equal(responseXml)
          }

        }

    }

  }

}

case class Person(name: String, age: Int, location: String, hobbies: List[String])

/**
  * The point of these tests are that you create your test using a piece of client code
  * that does the real call to a mocked endpoint. This is a dummy client used here as an
  * example of what that might look like.
  */
object SimpleClient {

  def doGetRequest(baseUrl: String, endPoint: String, headers: Map[String, String]): SimpleResponse = {
    val request = Http(baseUrl + endPoint).headers(headers)

    doRequest(request)
  }

  def doPostRequest(baseUrl: String, endPoint: String, headers: Map[String, String], body: String): SimpleResponse = {
    val request = Http(baseUrl + endPoint)
      .headers(headers)
      .postData(body)

    doRequest(request)
  }

  def doRequest(request: HttpRequest): SimpleResponse = {
    try {
      val response = request.asString

      if(!response.is2xx) {
        println("Request: \n" +  request)
        println("Response: \n" + response)
      }

      SimpleResponse(response.code, response.headers, response.body)
    } catch {
      case e: Throwable =>
        SimpleResponse(500, Map(), e.getMessage)
    }
  }

  case class SimpleResponse(status: Int, headers: Map[String, String], body: String)

}
