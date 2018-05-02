package com.itv.scalapact.http4s17.impl

import java.util.concurrent.Executors

import com.itv.scalapact.http4s17.impl.HeaderImplicitConversions._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter}
import com.itv.scalapact.shared._
import fs2.{Strategy, Task}
import javax.net.ssl.SSLContext
import org.http4s.dsl._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpService, Request, Response, Status}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

private object PactStubService {

  implicit class BlazeBuilderPimper(blazeBuilder: BlazeBuilder) {
    def withOptionalSsl(sslContext: Option[SSLContext]): BlazeBuilder =
      sslContext.fold(blazeBuilder)(ssl => blazeBuilder.withSSLContext(ssl))
  }

  def createServer(
      interactionManager: IInteractionManager,
      connectionPoolSize: Int,
      sslContextName: Option[String],
      port: Option[Int],
      config: ScalaPactSettings
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, sslContextMap: SslContextMap): BlazeBuilder = {
    val executorService = Executors.newFixedThreadPool(2)

    implicit val strategy: Strategy =
      Strategy.fromExecutionContext(ExecutionContext.fromExecutorService(executorService))

    BlazeBuilder
      .bindHttp(port.getOrElse(config.givePort), config.giveHost)
      .withExecutionContext(ExecutionContext.fromExecutorService(executorService))
      .withIdleTimeout(60.seconds)
      .withOptionalSsl(sslContextName)
      .withConnectorPoolSize(connectionPoolSize)
      .mountService(PactStubService.service(interactionManager, config.giveStrictMode), "/")
  }

  private val isAdminCall: Request => Boolean = request =>
    request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  private def service(
      interactionManager: IInteractionManager,
      strictMatching: Boolean
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, strategy: Strategy): HttpService =
    HttpService.apply { matchRequestWithResponse(interactionManager, strictMatching) }

  private def matchRequestWithResponse(interactionManager: IInteractionManager, strictMatching: Boolean)(
      implicit pactReader: IPactReader,
      pactWriter: IPactWriter,
      strategy: Strategy
  ): PartialFunction[Request, Task[Response]] = {
    case req =>
      if (isAdminCall(req)) {

        req.method.name.toUpperCase match {
          case m if m == "GET" && req.pathInfo.startsWith("/stub/status") =>
            Ok()

          case m if m == "GET" && req.pathInfo.startsWith("/interactions") =>
            val output =
              pactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), interactionManager.getInteractions))
            Ok(output)

          case m if m == "POST" || m == "PUT" && req.pathInfo.startsWith("/interactions") =>
            pactReader.jsonStringToPact(
              req.bodyAsText.runLog.map(body => Option(body.mkString)).unsafeRun().getOrElse("")
            ) match {
              case Right(r) =>
                interactionManager.addInteractions(r.interactions)

                val output =
                  pactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), interactionManager.getInteractions))
                Ok(output)

              case Left(l) =>
                InternalServerError(l)
            }

          case m if m == "DELETE" && req.pathInfo.startsWith("/interactions") =>
            interactionManager.clearInteractions()

            val output =
              pactWriter.pactToJsonString(Pact(PactActor(""), PactActor(""), interactionManager.getInteractions))
            Ok(output)
        }

      } else {
        interactionManager.findMatchingInteraction(
          InteractionRequest(
            method = Option(req.method.name.toUpperCase),
            headers = req.headers,
            query =
              if (req.params.isEmpty) None else Option(req.params.toList.map(p => p._1 + "=" + p._2).mkString("&")),
            path = Option(req.pathInfo),
            body = req.bodyAsText.runLog.map(body => Option(body.mkString)).unsafeRun(),
            matchingRules = None
          ),
          strictMatching = strictMatching
        ) match {
          case Right(ir) =>
            Status.fromInt(ir.response.status.getOrElse(200)) match {
              case Right(_) =>
                Http4sRequestResponseFactory.buildResponse(
                  status = IntAndReason(ir.response.status.getOrElse(200), None),
                  headers = ir.response.headers.getOrElse(Map.empty),
                  body = ir.response.body
                )

              case Left(l) =>
                InternalServerError(l.sanitized)
            }

          case Left(message) =>
            Http4sRequestResponseFactory.buildResponse(
              status = IntAndReason(598, Some("Pact Match Failure")),
              headers = Map("X-Pact-Admin" -> "Pact Match Failure"),
              body = Option(message)
            )
        }

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
    instance.foreach(_.shutdown.unsafeRunSync())
    instance = None
  }

}
