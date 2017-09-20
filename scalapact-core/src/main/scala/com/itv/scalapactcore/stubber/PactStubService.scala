package com.itv.scalapactcore.stubber

import java.util.concurrent.{ExecutorService, Executors}

import com.itv.scalapactcore._
import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.common.pact._
import com.itv.scalapactcore.common.{Arguments, Http4sRequestResponseFactory}
import org.http4s.dsl._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpService, Request, Response, Status}

import scala.concurrent.duration._

object PactStubService {

  private val nThreads: Int = 50
  private val executorService: ExecutorService = Executors.newFixedThreadPool(nThreads)

  def startServer: InteractionManager => Arguments => Unit = interactionManager => config => {
    println(("Starting ScalaPact Stubber on: http://" + config.giveHost + ":" + config.givePort).white.bold)
    println(("Strict matching mode: " + config.giveStrictMode).white.bold)

    runServer(interactionManager)(nThreads)(config).awaitShutdown()
  }

  def runServer: InteractionManager => Int => Arguments => Server = interactionManager => connectionPoolSize => config => {
    BlazeBuilder
      .bindHttp(config.givePort, config.giveHost)
      .withServiceExecutor(executorService)
      .withIdleTimeout(60.seconds)
      .withConnectorPoolSize(connectionPoolSize)
      .mountService(PactStubService.service(interactionManager)(config.giveStrictMode), "/")
      .run
  }

  def stopServer: Server => Unit = server => {
    server.shutdown.unsafePerformSync
  }

  private val isAdminCall: Request => Boolean = request =>
      request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  private val service: InteractionManager => Boolean => HttpService = interactionManager => strictMatching =>
    HttpService.lift { req =>
      matchRequestWithResponse(interactionManager, strictMatching)(req)
    }

  private val pactMatchFailureStatus: Status = Status.fromIntAndReason(598, "Pact Match Failure").toOption.getOrElse(InternalServerError)

  private def matchRequestWithResponse(interactionManager: InteractionManager, strictMatching: Boolean)(req: Request): scalaz.concurrent.Task[Response] = {
    if(isAdminCall(req)) {

      req.method.name.toUpperCase match {
        case m if m == "GET" && req.pathInfo.startsWith("/stub/status") =>
          Ok()

        case m if m == "GET" && req.pathInfo.startsWith("/interactions") =>
          val output = PactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), interactionManager.getInteractions))
          Ok(output)

        case m if m == "POST" || m == "PUT" && req.pathInfo.startsWith("/interactions") =>
          PactReader.jsonStringToPact(req.bodyAsText.runLog.map(body => Option(body.mkString)).unsafePerformSync.getOrElse("")) match {
            case Right(r) =>
              interactionManager.addInteractions(r.interactions)

              val output = PactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), interactionManager.getInteractions))
              Ok(output)

            case Left(l) =>
              InternalServerError(l)
          }

        case m if m == "DELETE" && req.pathInfo.startsWith("/interactions") =>
          interactionManager.clearInteractions()

          val output = PactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), interactionManager.getInteractions))
          Ok(output)
      }

    }
    else {

      import com.itv.scalapactcore.common.HeaderImplicitConversions._

      interactionManager.findMatchingInteraction(
        InteractionRequest(
          method = Option(req.method.name.toUpperCase),
          headers = Option(req.headers),
          query = if(req.params.isEmpty) None else Option(req.params.toList.map(p => p._1 + "=" + p._2).mkString("&")),
          path = Option(req.pathInfo),
          body = req.bodyAsText.runLog.map(body => Option(body.mkString)).unsafePerformSync,
          matchingRules = None
        ),
        strictMatching = strictMatching
      ) match {
        case Right(ir) =>
          Status.fromInt(ir.response.status.getOrElse(200)).toEither match {
            case Right(code) =>
              Http4sRequestResponseFactory.buildResponse(
                status = code,
                headers = ir.response.headers.getOrElse(Map.empty),
                body = ir.response.body
              )

            case Left(l) => InternalServerError(l.sanitized)
          }

        case Left(message) =>
          Http4sRequestResponseFactory.buildResponse(
            status = pactMatchFailureStatus,
            headers = Map("X-Pact-Admin" -> "Pact Match Failure"),
            body = Option(message)
          )
      }

    }
  }
}
