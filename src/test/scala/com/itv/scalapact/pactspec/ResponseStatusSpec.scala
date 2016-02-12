package com.itv.scalapact.pactspec

import org.scalatest.{FunSpec, Matchers}

import scalaj.http.Http
import com.itv.scalapact._
import com.itv.scalapact.pactspec.util.PactSpecLoader

import ScalaPactForger._

class ResponseStatusSpec extends FunSpec with Matchers {

  private val fetchSpec: String => StatusSpec = path =>
    PactSpecLoader.parseInto[StatusSpec](PactSpecLoader.fromResource(path))

  describe("Exercising response status test cases for the V2 Pact Specification") {

    it("should check the specs") {

      val specFiles = List(
        fetchSpec("response/status/different status.json"),
        fetchSpec("response/status/matches.json")
      )

      specFiles.zipWithIndex.foreach { specAndIndex =>

        val spec = specAndIndex._1
        val index = specAndIndex._2

        val endPoint = "/test-" + index

        forgePact
          .between("consumer-response-status")
          .and("provider-response-status")
          .describing("running status spec tests")
          .addInteraction(
            interaction
              .description(spec.comment)
              .uponReceiving(endPoint)
              .willRespondWith(spec.actual.status)
          )
          .runConsumerTest { mockConfig =>

            withClue(spec.comment + "\n") {
              val r = Http(mockConfig.baseUrl + endPoint).asString

              (r.code == spec.expected.status) should equal(spec.`match`)
            }

          }

      }

    }

  }

  case class StatusSpec(`match`: Boolean, comment: String, expected: StatusSpecResponse, actual: StatusSpecResponse)
  case class StatusSpecResponse(status: Int)

}
