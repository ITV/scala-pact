package com.itv.scalapact

import com.itv.scalapact.ScalaPactForger._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/** Strict and non-strict pacts cannot be mixed.
  */
class StrictExampleSpec extends AnyFunSpec with Matchers {

  import com.itv.scalapact.json._
  import com.itv.scalapact.http._

  describe("Example of a strict CDC Integration test") {

    it("Should be able to perform a strict match") {

      val endPoint = "/strict-match"

      val json: String => Int => List[String] => String = name =>
        count =>
          colours => {
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
        .between("My Strict Consumer")
        .and("Their Strict Provider Service")
        .addInteraction(
          interaction
            .description("a strict match")
            .uponReceiving(
              method = POST,
              path = endPoint,
              query = None,
              headers = Map.empty,
              body = json("Fred")(10)(List("red", "blue"))
            )
            .willRespondWith(
              status = 200,
              headers = Map.empty,
              body = "Success"
            )
        )
        .runConsumerTest { mockConfig =>
          // Note that the only difference is the array order
          val result =
            SimpleClient.doPostRequest(mockConfig.baseUrl, endPoint, Map.empty, json("Fred")(10)(List("blue", "red")))

          result.status should equal(598)
          result.headers.get("X-Pact-Admin") shouldEqual Some("Pact Match Failure")
          result.body.contains("Failed to match") shouldEqual true

        }

    }

  }

}
