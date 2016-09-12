package com.itv.scalapactcore.stubber

import com.itv.scalapactcore._
import com.itv.scalapactcore.common.Arguments
import com.itv.scalapactcore.common.ColourOuput._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpService, Request, Response, Status}

import scalaz.{-\/, \/-}

object PactStubService {

  lazy val startServer: Arguments => Unit = config => {
    println(("Starting ScalaPact Stubber on: http://" + config.giveHost + ":" + config.givePort).white.bold)

    BlazeBuilder.bindHttp(config.givePort, config.giveHost)
      .mountService(PactStubService.service, "/")
      .run
      .awaitShutdown()
  }

  private val isAdminCall: Request => Boolean = request =>
    request.pathInfo.startsWith("/interactions") &&
      request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  private val service = HttpService {
    case req => matchRequestWithResponse(req)
  }

  private def matchRequestWithResponse(req: Request): scalaz.concurrent.Task[Response] = {
    if(isAdminCall(req)) {

      req.method.name.toUpperCase match {
        case m if m == "GET" =>
          val output = ScalaPactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), InteractionManager.getInteractions))
          Ok(output)

        case m if m == "POST" || m == "PUT" =>
          ScalaPactReader.jsonStringToPact(req.bodyAsText.runLog.map(body => Option(body.foldLeft("")(_ + _))).unsafePerformSync.getOrElse("")) match {
            case \/-(r) =>
              InteractionManager.addInteractions(r.interactions)

              val output = ScalaPactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), InteractionManager.getInteractions))
              Ok(output)

            case -\/(l) =>
              InternalServerError(l)
          }

        case m if m == "DELETE" =>
          InteractionManager.clearInteractions()

          val output = ScalaPactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), InteractionManager.getInteractions))
          Ok(output)
      }

    }
    else {

      import HeaderImplicitConversions._

      InteractionManager.findMatchingInteraction(
        InteractionRequest(
          method = Option(req.method.name.toUpperCase),
          headers = Option(req.headers),
          query = if(req.params.isEmpty) None else Option(req.params.toList.map(p => p._1 + "=" + p._2).mkString("&")),
          path = Option(req.pathInfo),
          body = req.bodyAsText.runLast.unsafePerformSync,
          matchingRules = None
        ),
        strictMatching = false
      ) match {
        case \/-(ir) =>
          Status.fromInt(ir.response.status.getOrElse(200)) match {
            case \/-(code) =>
              Http4sRequestResponseFactory.buildResponse(
                status = code,
                headers = ir.response.headers.getOrElse(Map.empty),
                body = ir.response.body
              )

            case -\/(l) => InternalServerError(l.sanitized)
          }

        case -\/(message) =>
          NotFound(message)
      }

    }
  }
}
