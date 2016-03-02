package com.itv.scalapact.plugin.stubber

import argonaut.PrettyParams
import com.itv.scalapactcore.{ScalaPactWriter, PactActor, Pact}
import org.http4s.dsl.{->, /, Root, _}
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpService, Request, Response, Status}

import com.itv.scalapactcore.PactImplicits._

import scalaz.{-\/, \/-}

object PactStubService {

  lazy val startServer: Arguments => Unit = config => {
    println("Starting ScalaPact Stubber on: http://" + config.host + ":" + config.port)

    BlazeBuilder.bindHttp(config.port, config.host)
      .mountService(PactStubService.service, "/")
      .run
      .awaitShutdown()
  }

  private val isAdminCall: Request => Boolean = request =>
    request.pathInfo.startsWith("/interactions") &&
      request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  private val service = HttpService {

    case req @ GET -> Root / path =>
      matchRequestWithResponse(req)

    case req @ PUT -> Root / path =>
      matchRequestWithResponse(req)

    case req @ POST -> Root / path =>
      matchRequestWithResponse(req)

    case req @ DELETE -> Root / path =>
      matchRequestWithResponse(req)

  }

  private def matchRequestWithResponse(req: Request): scalaz.concurrent.Task[Response] = {
    if(isAdminCall(req)) {
      //TODO: Different admin calls should do different things...
      val output = ScalaPactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), InteractionManager.getInteractions))
      Ok(output)
    }
    else {

      import HeaderImplicitConversions._

      val rd = RequestDetails(
        method = Option(req.method.name.toUpperCase),
        headers = Option(req.headers),
        path = Option(req.pathInfo),
        body = req.bodyAsText.runLast.run
      )

      val interaction = InteractionManager.findMatchingInteraction(rd)

      if(interaction.isEmpty) NotFound("No interaction found for request: " + req.method.name.toUpperCase + " " + req.pathInfo)
      else {

        val i = interaction.get

        Status.fromInt(i.response.status.getOrElse(200)) match {
          case \/-(code) =>
            Http4sRequestResponseFactory.buildResponse(
              status = code,
              headers = i.response.headers.getOrElse(Map.empty),
              body = i.response.body
            )

          case -\/(l) => InternalServerError(l.sanitized)
        }
      }
    }
  }
}
