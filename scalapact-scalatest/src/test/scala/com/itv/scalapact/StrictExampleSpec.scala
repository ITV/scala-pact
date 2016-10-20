package com.itv.scalapact

import scala.language.implicitConversions

import org.scalatest.{FunSpec, Matchers}

import scalaj.http.{Http, HttpRequest}

import ScalaPactForger._

/**
 * Strict and non-strict pacts cannot be mixed.
 **/
class StrictExampleSpec extends FunSpec with Matchers {

  describe("Example of a strict CDC Integration test") {

    it("Should be able to perform a strict match") {

      val endPoint = "/strict-match"

      val json: String => Int => List[String] => String = name => count => colours => {
        s"""
          |{
          |  "name" : "$name",
          |  "count" : $count,
          |  "colours" : [${colours.map(s => "\"" + s + "\"").mkString(", ")}]
          |}
        """.stripMargin
      }

      // Different builder
      forgeStrictPact
        .between("My Consumer")
        .and("Their Provider Service")
        .addInteraction(
          interaction
            .description("a strict match")
            .uponReceiving(
              method = POST,
              path = endPoint,
              query = None,
              headers = Map.empty,
              body = json("Fred")(10)(List("red", "blue")),
              matchingRules = None
            )
            .willRespondWith(
              status = 200,
              headers = Map.empty,
              body = "Success",
              matchingRules = None
            )
        )
        .runConsumerTest { mockConfig =>

          // Note that the only difference is the array order
          val result = SimpleClient.doPostRequest(mockConfig.baseUrl, endPoint, Map.empty, json("Fred")(10)(List("blue", "red")))

          result.status should equal(404)

        }

    }

  }

}
