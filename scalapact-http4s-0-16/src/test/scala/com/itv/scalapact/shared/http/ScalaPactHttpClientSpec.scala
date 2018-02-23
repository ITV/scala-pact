package com.itv.scalapact.shared.http

import com.itv.scalapact.shared._
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._
import scalaz.concurrent.Task

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

      implicit val fakeCaller: SimpleRequest => Task[SimpleResponse] = _ => Task.now(SimpleResponse(200))

      val result = ScalaPactHttpClient.doInteractionRequest("", requestDetails, 1.second, sslContextName = None).run

      result shouldEqual responseDetails

    }

  }

}
