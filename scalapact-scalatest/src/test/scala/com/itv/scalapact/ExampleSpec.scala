package com.itv.scalapact

import scala.language.implicitConversions

import org.json4s.DefaultFormats
import org.json4s.native.JsonParser._
import org.json4s.native.Serialization._
import org.scalatest.{FunSpec, Matchers}

import scala.xml.XML
import scalaj.http.{Http, HttpRequest}

import ScalaPactForger._

class ExampleSpec extends FunSpec with Matchers {

  private implicit val formats = DefaultFormats

  describe("Example CDC Integration tests") {

   it("Should be able to create a contract for a simple GET") {

     val endPoint = "/hello"

     forgePact
       .between("My Consumer")
       .and("Their Provider Service")
       .addInteraction(
         interaction
           .description("a simple get example")
           .uponReceiving(endPoint)
           .willRespondWith(200, "Hello there!")
       )
       .runConsumerTest { mockConfig =>

         val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map.empty)

         result.status should equal(200)
         result.body should equal("Hello there!")

       }

   }

   it("Should be able to create a contract for a GET request with query params") {

     val endPoint = "/hello?location=london"

     forgePact
       .between("My Consumer")
       .and("Their Provider Service")
       .addInteraction(
         interaction
           .description("a get example with query parameters")
           .uponReceiving(GET, endPoint, "id=1&name=joe")
           .willRespondWith(200, "Hello there!")
       )
       .runConsumerTest { mockConfig =>

         val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint + "&id=1&name=joe", Map.empty)

         result.status should equal(200)
         result.body should equal("Hello there!")

       }

   }

   it("Should be able to create a contract for a simple GET with arbitrary headers") {

     val endPoint = "/hello2"

     forgePact
       .between("My Consumer")
       .and("Their Provider Service")
       .addInteraction(
         interaction
           .description("a simple get example with a header")
           .uponReceiving(GET, endPoint, None, Map("fish" -> "chips"), None, None)
           .willRespondWith(200, "Hello there!")
       )
       .runConsumerTest { mockConfig =>

         val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map("fish" -> "chips"))

         result.status should equal(200)
         result.body should equal("Hello there!")

       }

   }

   it("Should be able to create a contract for a GET request for JSON data") {

     val data = Person("Joe", 25, "London", List("Fishing", "Karate"))

     val endPoint = "/json"

     forgePact
       .between("My Consumer")
       .and("Their Provider Service")
       .addInteraction(
         interaction
           .description("Request for some json")
           .uponReceiving(endPoint)
           .willRespondWith(200, Map("Content-Type" -> "application/json"), write(data), None)
       )
       .runConsumerTest { mockConfig =>

         val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map())

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

     forgePact
       .between("My Consumer")
       .and("Their Provider Service")
       .addInteraction(
         interaction
           .description("POST JSON receive XML example")
           .uponReceiving(POST, endPoint, None, headers, write(requestData), None)
           .willRespondWith(400, Map("Content-Type" -> "text/xml"), responseXml.toString(), None)
       )
       .runConsumerTest { mockConfig =>

         val result = SimpleClient.doPostRequest(mockConfig.baseUrl, endPoint, headers, write(requestData))

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

   it("Should be able to declare the state the provider must be in before verification") {

     val endPoint = "/provider-state?id=1234"

     forgePact
       .between("My Consumer")
       .and("Their Provider Service")
       .addInteraction(
         interaction
           .description("Fetching a specific ID")
           .given("Resource with ID 1234 exists")
           .uponReceiving(endPoint)
           .willRespondWith(200, "ID: 1234 Exists")
       )
       .runConsumerTest { mockConfig =>

         val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map.empty)

         result.status should equal(200)
         result.body should equal("ID: 1234 Exists")

       }

   }

   it("Should be able to create a contract for a GET with header matchers") {

     val endPoint = "/header-match"

     forgePact
       .between("My Consumer")
       .and("Their Provider Service")
       .addInteraction(
         interaction
           .description("a simple get example with a header matcher")
           .uponReceiving(
             method = GET,
             path = endPoint,
             query = None,
             headers = Map("fish" -> "chips", "sauce" -> "ketchup"),
             body = None,
             matchingRules =
               headerRegexRule("fish", "\\w+") ~> headerRegexRule("sauce", "\\w+")
           )
           .willRespondWith(
             status = 200,
             headers = Map("fish" -> "chips", "sauce" -> "ketchup"),
             body = "Hello there!",
             matchingRules =
               headerRegexRule("fish", "\\w+") ~> headerRegexRule("sauce", "\\w+")
           )
       )
       .runConsumerTest { mockConfig =>

         val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map("fish" -> "peas", "sauce" -> "mayo"))

         result.status should equal(200)
         result.body should equal("Hello there!")

       }

    }

    it("Should be able to create a contract with a simple body matcher for the request and response") {

      val endPoint = "/body-match"

      val json: String => Int => List[String] => String = name => count => colours => {
        s"""
          |{
          |  "name" : "$name",
          |  "count" : $count,
          |  "colours" : [${colours.map(s => "\"" + s + "\"").mkString(", ")}]
          |}
        """.stripMargin
      }

      forgePact
        .between("My Consumer")
        .and("Their Provider Service")
        .addInteraction(
          interaction
            .description("a simple get example with body matchers")
            .uponReceiving(
              method = POST,
              path = endPoint,
              query = None,
              headers = Map.empty,
              body = json("Fred")(10)(List("red", "blue")),
              matchingRules =
                bodyRegexRule("name", "\\w+")
                ~> bodyTypeRule("count")
                ~> bodyArrayMinimumLengthRule("colours", 1)
            )
            .willRespondWith(
              status = 200,
              headers = Map.empty,
              body = "Success",
              matchingRules = None
            )
        )
        .runConsumerTest { mockConfig =>

          val result = SimpleClient.doPostRequest(mockConfig.baseUrl, endPoint, Map.empty, json("Sally")(5)(List("red")))

          result.status should equal(200)
          result.body should equal("Success")

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

  implicit def convertHeaders(headers: Map[String, IndexedSeq[String]]): Map[String, String] =
    headers.map { h => (h._1, h._2.headOption.getOrElse("")) }

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

//      if(!response.is2xx) {
//        println("Request: \n" +  request)
//        println("Response: \n" + response)
//      }

      SimpleResponse(response.code, response.headers, response.body)
    } catch {
      case e: Throwable =>
        SimpleResponse(500, Map(), e.getMessage)
    }
  }

  case class SimpleResponse(status: Int, headers: Map[String, String], body: String)

}
