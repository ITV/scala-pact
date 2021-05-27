package com.itv.scalapact.http4s.http4s23.impl

import cats.effect.unsafe.implicits.global
import com.itv.scalapact.http4s23.impl.{Http4sRequestResponseFactory, IntAndReason}
import com.itv.scalapact.shared.http.{HttpMethod, SimpleRequest}
import org.scalatest.{FunSpec, Matchers}

class Http4sRequestResponseFactorySpec extends FunSpec with Matchers {

  describe("Creating Http4s requests and responses") {

    it("should be able to manufacture a good request") {

      val simpleRequest = SimpleRequest(
        "http://localhost:8080",
        "/foo",
        HttpMethod.POST,
        Map(
          "Accept"       -> "application/json",
          "Content-Type" -> "test/plain"
        ),
        Some("Greetings!"),
        None
      )

      val request = Http4sRequestResponseFactory.buildRequest(simpleRequest).unsafeRunSync()

      request.method.name shouldEqual "POST"
      request.pathInfo.renderString.contains("foo") shouldEqual true
      request.bodyText.compile.toVector.unsafeRunSync().mkString shouldEqual "Greetings!"

    }

    it("should be able to manufacture a good response") {

      val response = Http4sRequestResponseFactory
        .buildResponse(
          IntAndReason(404, Some("Not Found")),
          Map(
            "Content-Type" -> "test/plain"
          ),
          Some("Missing")
        )

      response.status.code shouldEqual 404
      response.bodyText.compile.toVector.unsafeRunSync().mkString shouldEqual "Missing"

    }

  }

}
