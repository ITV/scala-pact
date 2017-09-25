package com.itv.scalapact.shared.http

import com.itv.scalapact.shared.HttpMethod
import org.scalatest.{FunSpec, Matchers}

class Http4sRequestResponseFactorySpec extends FunSpec with Matchers {

  describe("Creating Http4s requests and responses") {

    it("should be able to manufacture a good request") {

      val request = Http4sRequestResponseFactory.buildRequest(
        HttpMethod.POST,
        "http://localhost:8080",
        "/foo",
        Map(
          "Accept" -> "application/json",
          "Content-Type" -> "test/plain"
        ),
        Some("Greetings!")
      ).unsafePerformSync

      request.method.name shouldEqual "GET"

    }

    it("should be able to manufacture a good response") {
      pending
    }

  }

}
