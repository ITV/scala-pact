package com.itv.scalapact

import org.scalatest.{FunSpec, Matchers}

import ScalaPactForger._

class HeavySpec extends FunSpec with Matchers {

  import com.itv.scalapact.json._
  import com.itv.scalapact.http._

  def makeEndPoints(name: String, count: Int): List[String] = (0 until count).toList.map(i => s"/$name/_$i")

  def generatePactAndTestIt(endPoint: String): Unit =
    forgePact
      .between("heavy-consumer")
      .and("heavy-provider")
      .addInteraction(interaction.description("load").uponReceiving(endPoint).willRespondWith(200))
      .runConsumerTest { config =>
        val response = SimpleClient.doGetRequest(config.baseUrl, endPoint, Map())

        withClue("Failing response: " + response) {
          response.status shouldEqual 200
        }
      }

  describe("Running many pacts") {

    it("should be able to cope with a pact test") {
      makeEndPoints("single", 1).foreach(generatePactAndTestIt)
    }

    it("should be able to cope with another 10 pact tests") {
      makeEndPoints("ten", 10).foreach(generatePactAndTestIt)
    }

    it("should be able to cope with another 20 pact tests") {
      makeEndPoints("twenty", 20).foreach(generatePactAndTestIt)
    }

    it("should be able to cope with another 100 pact tests") {
      makeEndPoints("one-hundred", 100).foreach(generatePactAndTestIt)
    }

  }

}
