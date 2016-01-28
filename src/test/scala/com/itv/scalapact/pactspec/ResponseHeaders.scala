package com.itv.scalapact.pactspec

import org.scalatest.{Matchers, FunSpec}

import scalaj.http.Http
import com.itv.scalapact._
import com.itv.scalapact.pactspec.util.PactSpecLoader

class ResponseHeaders extends FunSpec with Matchers {

  private val fetchSpec: String => HeaderSpec = path =>
    PactSpecLoader.parseInto[HeaderSpec](PactSpecLoader.fromResource(path))

  describe("Exercising response header test cases for the V2 Pact Specification") {
/*
    it("should check the specs") {

      val specFiles = List(
        fetchSpec("response/headers/whitespace after comma different.json")
      )

      specFiles.zipWithIndex.foreach { specAndIndex =>

        val spec = specAndIndex._1
        val index = specAndIndex._2

        val endPoint = "/test-" + index

        PactBuilder
          .consumer("consumer")
          .hasPactWith("provider")
          .withOptions(ScalaPactOptions(writePactFiles = false))
          .withInteraction(
            PactInteraction(
              description = spec.comment,
              given = None,
              uponReceivingRequest
                .path(endPoint),
              willRespondWith
                .status(200)
                .headers(spec.actual.headers)
            )
          )
          .withConsumerTest { scalaPactMockConfig =>

            withClue(spec.comment + "\n") {
              val r = Http(scalaPactMockConfig.baseUrl + endPoint).asString

              (r.headers == spec.expected.headers) should equal(spec.`match`)
            }

          }
      }

    }*/

  }

  case class HeaderSpec(`match`: Boolean, comment: String, expected: HeaderSpecResponse, actual: HeaderSpecResponse)
  case class HeaderSpecResponse(headers: Map[String, String])

}
