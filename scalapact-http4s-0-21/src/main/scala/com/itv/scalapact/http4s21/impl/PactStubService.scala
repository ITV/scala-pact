package com.itv.scalapact.http4s21.impl

import java.util.concurrent.Executors

import cats.data._
import cats.effect._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter}
import javax.net.ssl.SSLContext
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpApp, Request, Response, Status}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PactStubService {

  implicit class BlazeBuilderPimper(blazeBuilder: BlazeServerBuilder[IO]) {
    def withOptionalSsl(sslContext: Option[SSLContext]): BlazeServerBuilder[IO] =
      sslContext.fold(blazeBuilder)(ssl => blazeBuilder.withSSLContext(ssl))
  }

  def createServer(
      interactionManager: IInteractionManager,
      connectionPoolSize: Int,
      sslContextName: Option[String],
      port: Option[Int],
      config: ScalaPactSettings
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, sslContextMap: SslContextMap): BlazeServerBuilder[IO] = {

    val executionContext: ExecutionContext =
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
    implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)
    implicit val timer: Timer[IO]     = IO.timer(executionContext)

    BlazeServerBuilder[IO]
      .bindHttp(port.getOrElse(config.givePort), config.giveHost)
      .withExecutionContext(executionContext)
      .withIdleTimeout(60.seconds)
      .withOptionalSsl(sslContextName)
      .withConnectorPoolSize(connectionPoolSize)
      .withHttpApp(PactStubService.service(interactionManager, config.giveStrictMode))
  }

  private val isAdminCall: Request[IO] => Boolean = request =>
    request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  def service(
      interactionManager: IInteractionManager,
      strictMatching: Boolean
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter): HttpApp[IO] =
    Kleisli[IO, Request[IO], Response[IO]](matchRequestWithResponse(interactionManager, strictMatching, _))

  private def matchRequestWithResponse(
      interactionManager: IInteractionManager,
      strictMatching: Boolean,
      req: Request[IO]
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter): IO[Response[IO]] =
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
          req
            .attemptAs[String]
            .fold(_ => None, Option.apply)
            .map { x =>
              pactReader.jsonStringToPact(x.getOrElse(""))
            }
            .flatMap {
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
      req.attemptAs[String].fold(_ => None, Option.apply).flatMap { maybeBody =>
        interactionManager.findMatchingInteraction(
          InteractionRequest(
            method = Option(req.method.name.toUpperCase),
            headers = Option(req.headers.toList.map { h =>
              h.name.toString -> h.value
            }.toMap),
            query =
              if (req.params.isEmpty) None else Option(req.multiParams.toList.flatMap { case (key, values) => values.map((key,_))}.map(p => p._1 + "=" + p._2).mkString("&")),
            path = Option(req.pathInfo),
            body = maybeBody,
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
