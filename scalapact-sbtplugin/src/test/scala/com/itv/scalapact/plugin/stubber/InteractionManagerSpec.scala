package com.itv.scalapact.plugin.stubber

import com.itv.scalapactcore.{InteractionResponse, InteractionRequest, Interaction}

import scala.language.implicitConversions

import org.scalatest.{Matchers, FunSpec}

class InteractionManagerSpec extends FunSpec with Matchers {

  implicit def toOption[A](thing: A): Option[A] = Option(thing)

  describe("The interaction manager's matching of get requests") {

    it("should be able to match a simple get request") {
      val interactionManager = new InteractionManager {}

      val requestDetails = InteractionRequest(
        method = "GET",
        headers = None,
        query = None,
        path = "/foo",
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo",
          query = None,
          headers = None,
          body = None
        ),
        response = InteractionResponse(
          status = 200,
          headers = None,
          body = None
        )
      )

      interactionManager.addInteraction(interaction)

      val matched = interactionManager.findMatchingInteraction(requestDetails).toOption

      matched.isDefined shouldEqual true
      matched.get.response.status shouldEqual Some(200)
    }

    it("should be able to match a less simple get request") {
      val interactionManager = new InteractionManager {}

      val requestDetails = InteractionRequest(
        method = "GET",
        headers = Option(
          Map(
            "Upgrade-Insecure-Requests" -> "1",
            "Connection" -> "keep-alive",
            "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Cache-Control" -> "max-age=0",
            "Accept-Language" -> "en-US,en;q=0.8",
            "Accept-Encoding" -> "gzip",
            "deflate" -> "",
            "sdch" -> "",
            "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36",
            "Host" -> "localhost:1234"
          )
        ),
        path = "/foo",
        query = None,
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo",
          query = None,
          headers = None,
          body = None
        ),
        response = InteractionResponse(
          status = 200,
          headers = None,
          body = None
        )
      )

      interactionManager.addInteraction(interaction)

      val matched = interactionManager.findMatchingInteraction(requestDetails).toOption

      matched.isDefined shouldEqual true
      matched.get.response.status shouldEqual Some(200)
    }
  }

  describe("The interaction manager's matching of get requests with headers") {

    it("should be able to match a get request with a header") {
      val interactionManager = new InteractionManager {}

      val requestDetails1 = InteractionRequest(
        method = "GET",
        headers = Map("fish" -> "chips"),
        path = "/foo",
        query = None,
        body = None
      )
      val requestDetails2 = InteractionRequest(
        method = "GET",
        headers = Map("fish" -> "peas"),
        path = "/foo",
        query = None,
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo",
          query = None,
          headers = Map("fish" -> "chips"),
          body = None
        ),
        response = InteractionResponse(
          status = 200,
          headers = None,
          body = None
        )
      )

      interactionManager.addInteraction(interaction)

      val matched1 = interactionManager.findMatchingInteraction(requestDetails1).toOption

      matched1.isDefined shouldEqual true
      matched1.get.response.status shouldEqual Some(200)

      interactionManager.findMatchingInteraction(requestDetails2).toOption.isEmpty shouldEqual true
    }

    it("should be able to match a get request with lots of headers") {
      val interactionManager = new InteractionManager {}

      val requestDetails = InteractionRequest(
        method = "GET",
        headers = Map("Content-Type" -> "text/plain; charset=uft-8", "Content-Length" -> "0", "Accept" -> "application/json"),
        path = "/foo",
        query = None,
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo",
          query = None,
          headers = Map("Accept" -> "application/json"),
          body = None
        ),
        response = InteractionResponse(
          status = 200,
          headers = None,
          body = None
        )
      )

      interactionManager.addInteraction(interaction)

      val matched = interactionManager.findMatchingInteraction(requestDetails).toOption

      matched.isDefined shouldEqual true
      matched.get.response.status shouldEqual Some(200)
    }
  }

  describe("The interaction manager's matching of get requests with complicated paths") {

    it("should be able to match a get request with a longer path") {
      val interactionManager = new InteractionManager {}

      val goodRequestDetails = InteractionRequest(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello",
        query = None,
        body = None
      )

      val badRequestDetails = InteractionRequest(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello/",
        query = None,
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo/bar/hello",
          query = None,
          headers = None,
          body = None
        ),
        response = InteractionResponse(
          status = 200,
          headers = None,
          body = None
        )
      )

      interactionManager.addInteraction(interaction)

      val matched = interactionManager.findMatchingInteraction(goodRequestDetails).toOption

      matched.isDefined shouldEqual true
      matched.get.response.status shouldEqual Some(200)

      interactionManager.findMatchingInteraction(badRequestDetails).toOption.isEmpty shouldEqual true
    }

    it("should be able to match a get request with parameters") {
      val interactionManager = new InteractionManager {}

      val goodRequestDetails1 = InteractionRequest(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello?id=1234&name=joe",
        query = None,
        body = None
      )

      val goodRequestDetails2 = InteractionRequest(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello?name=joe&id=1234",
        query = None,
        body = None
      )

      val goodRequestDetails3 = InteractionRequest(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello?name=joe",
        query = Option("id=1234"),
        body = None
      )

      val goodRequestDetails4 = InteractionRequest(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello",
        query = Option("name=joe&id=1234"),
        body = None
      )

      val badRequestDetails1 = InteractionRequest(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello?name=joe&id=1234&occupation=troubleMaker",
        query = None,
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo/bar/hello?id=1234&name=joe",
          query = None,
          headers = None,
          body = None
        ),
        response = InteractionResponse(
          status = 200,
          headers = None,
          body = None
        )
      )

      interactionManager.addInteraction(interaction)

      withClue("goodRequestDetails1") {
        interactionManager.findMatchingInteraction(goodRequestDetails1).toOption.isDefined shouldEqual true
      }

      withClue("goodRequestDetails2") {
        interactionManager.findMatchingInteraction(goodRequestDetails2).toOption.isDefined shouldEqual true
      }

      withClue("goodRequestDetails3") {
        interactionManager.findMatchingInteraction(goodRequestDetails3).toOption.isDefined shouldEqual true
      }

      withClue("goodRequestDetails4") {
        interactionManager.findMatchingInteraction(goodRequestDetails4).toOption.isDefined shouldEqual true
      }

      withClue("badRequestDetails1") {
        //Forgiving in what you receive...
        interactionManager.findMatchingInteraction(badRequestDetails1).toOption.isDefined shouldEqual true
      }

    }

  }

}
