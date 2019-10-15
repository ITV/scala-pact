package com.itv.scalapact.http4s.http4s21.impl

import cats.effect.{IO, Resource}
import com.itv.scalapact.http4s21.impl.ScalaPactHttpClient
import com.itv.scalapact.shared._
import org.http4s.client.Client
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._

class ScalaPactHttpClientSpec extends FunSpec with Matchers {

  describe("Making an interaction request") {

    it("should be able to make and interaction request and get an interaction response") {

      val requestDetails = InteractionRequest(
        method = Some("GET"),
        headers = None,
        query = None,
        path = Some("/foo"),
        body = None,
        matchingRules = None
      )

      val responseDetails = InteractionResponse(
        status = Some(200),
        headers = None,
        body = None,
        matchingRules = None
      )

      val fakeCaller: (SimpleRequest, Resource[IO, Client[IO]]) => IO[SimpleResponse] =
        (_, _) => IO.pure(SimpleResponse(200))

      val result =
        new ScalaPactHttpClient(fakeCaller)
          .doInteractionRequestIO(fakeCaller, "", requestDetails, 1.second, None)
          .unsafeRunSync()

      result shouldEqual responseDetails
    }
  }
}
