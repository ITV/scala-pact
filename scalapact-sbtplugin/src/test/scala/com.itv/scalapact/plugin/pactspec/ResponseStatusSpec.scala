package com.itv.scalapact.plugin.pactspec

import com.itv.scalapact.plugin.common.InteractionMatchers._
import com.itv.scalapact.plugin.pactspec.util.{PactSpecLoader, ResponseSpec}
import com.itv.scalapactcore.{Interaction, InteractionRequest}
import org.scalatest.{FunSpec, Matchers}

import scalaz.{-\/, \/-}


class ResponseStatusSpec extends FunSpec with Matchers {

  private val fetchSpec: String => ResponseSpec = path =>
    PactSpecLoader.deserializeResponseSpec(PactSpecLoader.fromResource(path)).get

  describe("Exercising response V2 Pact Specification match tests") {

    it("should check the response status specs") {
      testSpecs(
        List(
          fetchSpec("/response/status/different status.json"),
          fetchSpec("/response/status/matches.json")
        )
      )
    }

    it("should check the response header specs") {
      testSpecs(
        List(
          fetchSpec("/response/headers/empty headers.json"),
          fetchSpec("/response/headers/header name is different case.json"),
          fetchSpec("/response/headers/header value is different case.json"),
          fetchSpec("/response/headers/matches.json"),
          fetchSpec("/response/headers/order of comma separated header values different.json"),
          fetchSpec("/response/headers/unexpected header found.json"),
          fetchSpec("/response/headers/whitespace after comma different.json")
        )
      )
    }

    it("should check the response header specs with regex") {
      pending
      testSpecs(
        List(
          fetchSpec("/response/headers/matches with regex.json")
        )
      )
    }

    it("should check the response body specs") {
      pending
    }
  }

  private def testSpecs(specFiles: List[ResponseSpec]): Unit = {
    specFiles.foreach { spec =>

      val i = Interaction(None, "", InteractionRequest(None, None, None, None, None), spec.expected)

      matchResponse(i :: Nil)(spec.actual) match {
        case \/-(r) =>
          if(spec.`match`) 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
          else fail(spec.comment)

        case -\/(l) =>
          if(spec.`match`) fail(spec.comment)
          else 1 shouldEqual 1 // It's here, so the test should pass. Can't find a 'pass' method...
      }

    }
  }

}
