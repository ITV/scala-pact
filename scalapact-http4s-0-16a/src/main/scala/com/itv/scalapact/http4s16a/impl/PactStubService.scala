package com.itv.scalapact.http4s16a.impl

import java.util.concurrent.Executors

import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter}
import com.itv.scalapact.shared.{ScalaPactSettings, _}
import javax.net.ssl.SSLContext
import org.http4s.dsl._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpService, Request, Response}
import scalaz.concurrent.Task

import scala.concurrent.duration._

private object PactStubService {

  implicit private class BlazeBuilderPimper(blazeBuilder: BlazeBuilder) {
    def withOptionalSsl(sslContext: Option[SSLContext]): BlazeBuilder =
      sslContext.fold(blazeBuilder)(ssl => blazeBuilder.withSSLContext(ssl))
  }

  def createServer(
                    interactionManager: IInteractionManager,
                    connectionPoolSize: Int,
                    sslContextName: Option[String],
                    port: Option[Int],
                    config: ScalaPactSettings
                  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, sslContextMap: SslContextMap): BlazeBuilder =
    BlazeBuilder
      .bindHttp(port.getOrElse(config.givePort), config.giveHost)
      .withServiceExecutor(Executors.newFixedThreadPool(2))
      .withIdleTimeout(60.seconds)
      .withOptionalSsl(sslContextName)
      .withConnectorPoolSize(connectionPoolSize)
      .mountService(PactStubService.service(interactionManager, config.giveStrictMode), "/")

  private val isAdminCall: Request => Boolean = request =>
    request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  private def service(interactionManager: IInteractionManager,
                      strictMatching: Boolean)(implicit pactReader: IPactReader, pactWriter: IPactWriter): HttpService =
    HttpService.lift { req =>
      matchRequestWithResponse(interactionManager, strictMatching, req)
    }

  private def matchRequestWithResponse(interactionManager: IInteractionManager, strictMatching: Boolean, req: Request)(
    implicit pactReader: IPactReader,
    pactWriter: IPactWriter
  ): Task[Response] =
    if (isAdminCall(req)) {

      req.method.name.toUpperCase match {
        case m if m == "GET" && req.pathInfo.startsWith("/stub/status") =>
          Ok()

        case m if m == "GET" && req.pathInfo.startsWith("/interactions") =>
          val output =
            pactWriter.pactToJsonString(
              Pact(PactActor(""), PactActor(""), interactionManager.getInteractions, None, None),
              BuildInfo.version
            )
          Ok(output)

        case m if m == "POST" || m == "PUT" && req.pathInfo.startsWith("/interactions") =>
          pactReader.jsonStringToPact(
            req.bodyAsText
              .runLog[Task, String]
              .map(body => Option(body.mkString))
              .unsafePerformSync
              .getOrElse("")
          ) match {
            case Right(r) =>
              interactionManager.addInteractions(r.interactions)

              val output =
                pactWriter.pactToJsonString(
                  Pact(PactActor(""), PactActor(""), interactionManager.getInteractions, None, None),
                  BuildInfo.version
                )
              Ok(output)

            case Left(l) =>
              InternalServerError(l)
          }

        case m if m == "DELETE" && req.pathInfo.startsWith("/interactions") =>
          interactionManager.clearInteractions()

          val output =
            pactWriter.pactToJsonString(
              Pact(PactActor(""), PactActor(""), interactionManager.getInteractions, None, None),
              BuildInfo.version
            )
          Ok(output)
      }

    } else {

      interactionManager.findMatchingInteraction(
        InteractionRequest(
          method = Option(req.method.name.toUpperCase),
          headers = Option(req.headers.asMap),
          query = if (req.multiParams.isEmpty) None else Option(req.multiParams.toList.flatMap { case (key, values) => values.map((key,_))}.map(p => p._1 + "=" + p._2).mkString("&")),
          path = Option(req.pathInfo),
          body = req.bodyAsText.runLog[Task, String].map(body => Option(body.mkString)).unsafePerformSync,
          matchingRules = None
        ),
        strictMatching = strictMatching
      ) match {
        case Right(ir) =>
          Http4sRequestResponseFactory.buildResponse(
            status = IntAndReason(ir.response.status.getOrElse(200), None),
            headers = ir.response.headers.getOrElse(Map.empty),
            body = ir.response.body
          )

        case Left(message) =>
          Http4sRequestResponseFactory.buildResponse(
            status = IntAndReason(598, Some("Pact Match Failure")),
            headers = Map("X-Pact-Admin" -> "Pact Match Failure"),
            body = Option(message)
          )
      }

    }
}

class PactServer extends IPactStubber {

  private var instance: Option[Server] = None

  private def blazeBuilder(
                            scalaPactSettings: ScalaPactSettings,
                            interactionManager: IInteractionManager,
                            connectionPoolSize: Int,
                            sslContextName: Option[String],
                            port: Option[Int]
                          )(implicit pactReader: IPactReader, pactWriter: IPactWriter, sslContextMap: SslContextMap): BlazeBuilder =
    PactStubService.createServer(
      interactionManager,
      connectionPoolSize,
      sslContextName,
      port,
      scalaPactSettings
    )

  def start(interactionManager: IInteractionManager,
            connectionPoolSize: Int,
            sslContextName: Option[String],
            port: Option[Int])(implicit pactReader: IPactReader,
                               pactWriter: IPactWriter,
                               sslContextMap: SslContextMap): ScalaPactSettings => IPactStubber =
    scalaPactSettings =>
      instance match {
        case Some(_) =>
          this

        case None =>
          instance = Option(
            blazeBuilder(scalaPactSettings, interactionManager, connectionPoolSize, sslContextName, port).run
          )
          this
      }

  def shutdown(): Unit = {
    instance.foreach(_.shutdownNow())
    instance = None
    ()
  }

  def port: Option[Int] = instance.map(_.address.getPort)
}
