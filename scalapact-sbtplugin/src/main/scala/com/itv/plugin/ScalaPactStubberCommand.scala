package com.itv.plugin

import argonaut.Argonaut._
import org.http4s._
import org.http4s.server._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import sbt._

object ScalaPactStubberCommand {
  lazy val pactStubberCommandHyphen: Command = Command.command("pact-stubber")(pactStubber)
  lazy val pactStubberCommandCamel: Command = Command.command("pactStubber")(pactStubber)

  private lazy val pactStubber: State => State = state => {

    println("Placeholder for ScalaPact stubber command")

    serverStart()
      .awaitShutdown()

    state
  }

  private def serverStart(): Server = {
    BlazeBuilder.bindHttp(1234)
      .mountService(PactStubService.service, "/")
      .run
  }
}

object PactStubService {
  val service = HttpService {

    //TODO: Must have admin header
    case GET -> Root / "interactions" =>
      Ok("GET interactions")

    //TODO: Must have admin header
    case PUT -> Root / "interactions" =>
      Ok("PUT interactions")

    //TODO: Must have admin header
    case POST -> Root / "interactions" =>
      Ok("POST interactions")

    //TODO: Must have admin header
    case DELETE -> Root / "interactions" =>
      Ok("DELETE interactions")

    case req @ GET -> Root / path =>
      Ok("GET " + req.pathInfo)

    case req @ PUT -> Root / path =>
      Ok("PUT " + req.pathInfo)

    case req @ POST -> Root / path =>
      Ok("POST " + req.pathInfo)

    case req @ DELETE -> Root / path =>
      Ok("DELETE " + req.pathInfo)

  }
}