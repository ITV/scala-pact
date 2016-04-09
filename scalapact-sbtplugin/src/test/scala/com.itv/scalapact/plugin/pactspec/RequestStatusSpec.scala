package com.itv.scalapact.plugin.pactspec

import com.itv.scalapact.plugin.common.InteractionMatchers._
import com.itv.scalapact.plugin.pactspec.util.{PactSpecLoader, RequestSpec}
import com.itv.scalapactcore.{Interaction, InteractionResponse}
import org.scalatest.{FunSpec, Matchers}

import scalaz.{-\/, \/-}


class RequestStatusSpec extends FunSpec with Matchers {

  private val fetchSpec: String => RequestSpec = path =>
    PactSpecLoader.deserializeRequestSpec(PactSpecLoader.fromResource(path)).get

  describe("Exercising response V2 Pact Specification match tests") {

    it("should check the request method specs") {
      testSpecs(
        List(
          fetchSpec("/request/method/different method.json"),
          fetchSpec("/request/method/matches.json"),
          fetchSpec("/request/method/method is different case.json")
        )
      )
    }

    it("should check the request path specs") {
      testSpecs(
        List(
          fetchSpec("/request/path/empty path found when forward slash expected.json"),
          fetchSpec("/request/path/forward slash found when empty path expected.json"),
          fetchSpec("/request/path/incorrect path.json"),
          fetchSpec("/request/path/matches.json"),
          fetchSpec("/request/path/missing trailing slash in path.json"),
          fetchSpec("/request/path/unexpected trailing slash in path.json")
        )
      )
    }

    it("should check the request query specs") {
      testSpecs(
        List(
          fetchSpec("/request/query/different order.json"),
          fetchSpec("/request/query/different params.json"),
          fetchSpec("/request/query/matches.json"),
          fetchSpec("/request/query/missing params.json"),
          fetchSpec("/request/query/same parameter different values.json"),
          fetchSpec("/request/query/same parameter multiple times in different order.json"),
          fetchSpec("/request/query/same parameter multiple times.json"),
          fetchSpec("/request/query/trailing ampersand.json"),
          fetchSpec("/request/query/unexpected param.json")
        )
      )
    }

    it("should check the request header specs") {
      testSpecs(
        List(
          fetchSpec("/request/headers/empty headers.json"),
          fetchSpec("/request/headers/header name is different case.json"),
          fetchSpec("/request/headers/header value is different case.json"),
          fetchSpec("/request/headers/matches.json"),
          fetchSpec("/request/headers/order of comma separated header values different.json"),
          fetchSpec("/request/headers/unexpected header found.json"),
          fetchSpec("/request/headers/whitespace after comma different.json")
        )
      )
    }

    it("should check the request header specs with regex") {
      testSpecs(
        List(
          fetchSpec("/request/headers/matches with regex.json")
        )
      )
    }

    it("should check the request body specs") {
      pending
    }
  }

  private def testSpecs(specFiles: List[RequestSpec]): Unit = {
    specFiles.foreach { spec =>

      val i = Interaction(None, "", spec.expected, InteractionResponse(None, None, None, None))

      matchRequest(i :: Nil)(spec.actual) match {
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
