package com.itv.scalapact.plugin.stubber

import com.itv.scalapactcore.{InteractionResponse, InteractionRequest, Interaction}

import scala.language.implicitConversions

import org.scalatest.{Matchers, FunSpec}

class InteractionManagerSpec extends FunSpec with Matchers {

  implicit def toOption[A](thing: A): Option[A] = Option(thing)

  describe("The interaction manager's matching of get requests") {

    it("should be able to match a simple get request") {
      val interactionManager = new InteractionManager {}

      val requestDetails = RequestDetails(
        method = "GET",
        headers = None,
        path = "/foo",
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo",
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

      val matched = interactionManager.findMatchingInteraction(requestDetails)

      matched.isDefined shouldEqual true
      matched.get.response.status shouldEqual Some(200)
    }
  }

  describe("The interaction manager's matching of get requests with headers") {

    it("should be able to match a get request with a header") {
      val interactionManager = new InteractionManager {}

      val requestDetails1 = RequestDetails(
        method = "GET",
        headers = Map("fish" -> "chips"),
        path = "/foo",
        body = None
      )
      val requestDetails2 = RequestDetails(
        method = "GET",
        headers = Map("fish" -> "peas"),
        path = "/foo",
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo",
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

      val matched1 = interactionManager.findMatchingInteraction(requestDetails1)

      matched1.isDefined shouldEqual true
      matched1.get.response.status shouldEqual Some(200)

      val matched2 = interactionManager.findMatchingInteraction(requestDetails2)

      matched2.isEmpty shouldEqual true
    }

    it("should be able to match a get request with lots of headers") {
      val interactionManager = new InteractionManager {}

      val requestDetails = RequestDetails(
        method = "GET",
        headers = Map("Content-Type" -> "text/plain; charset=uft-8", "Content-Length" -> "0", "Accept" -> "application/json"),
        path = "/foo",
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo",
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

      val matched = interactionManager.findMatchingInteraction(requestDetails)

      matched.isDefined shouldEqual true
      matched.get.response.status shouldEqual Some(200)
    }
  }

  describe("The interaction manager's matching of get requests with complicated paths") {

    it("should be able to match a get request with a longer path") {
      val interactionManager = new InteractionManager {}

      val goodRequestDetails = RequestDetails(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello",
        body = None
      )

      val badRequestDetails = RequestDetails(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello/",
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo/bar/hello",
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

      val matched = interactionManager.findMatchingInteraction(goodRequestDetails)

      matched.isDefined shouldEqual true
      matched.get.response.status shouldEqual Some(200)

      interactionManager.findMatchingInteraction(badRequestDetails).isEmpty shouldEqual true
    }

    it("should be able to match a get request with parameters") {
      val interactionManager = new InteractionManager {}

      val goodRequestDetails1 = RequestDetails(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello?id=1234&name=joe",
        body = None
      )

      val goodRequestDetails2 = RequestDetails(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello?name=joe&id=1234",
        body = None
      )
      val badRequestDetails1 = RequestDetails(
        method = "GET",
        headers = None,
        path = "/foo/bar/hello?name=joe&id=1234&occupation=troubleMaker",
        body = None
      )

      val interaction = Interaction(
        providerState = None,
        description = "",
        request = InteractionRequest(
          method = "GET",
          path = "/foo/bar/hello?id=1234&name=joe",
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

      interactionManager.findMatchingInteraction(goodRequestDetails1).isDefined shouldEqual true
      interactionManager.findMatchingInteraction(goodRequestDetails2).isDefined shouldEqual true
      interactionManager.findMatchingInteraction(badRequestDetails1).isDefined shouldEqual false

    }

  }

}
