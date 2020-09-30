package com.itv.scalapact.http4s18.impl

import java.util.concurrent.Executors

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter}
import com.itv.scalapact.shared._
import javax.net.ssl.SSLContext
import org.http4s.dsl.io._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpService, Request, Response, Status}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PactStubService {
  type OptIO[A] = OptionT[IO, A]

  implicit class BlazeBuilderPimper(blazeBuilder: BlazeBuilder[IO]) {
    def withOptionalSsl(sslContext: Option[SSLContext]): BlazeBuilder[IO] =
      sslContext.fold(blazeBuilder)(ssl => blazeBuilder.withSSLContext(ssl))
  }

  def createServer(
      interactionManager: IInteractionManager,
      connectionPoolSize: Int,
      sslContextName: Option[String],
      port: Option[Int],
      config: ScalaPactSettings
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, sslContextMap: SslContextMap): BlazeBuilder[IO] = {

    val executionContext: ExecutionContext =
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

    BlazeBuilder[IO]
      .bindHttp(port.getOrElse(config.givePort), config.giveHost)
      .withExecutionContext(executionContext)
      .withIdleTimeout(60.seconds)
      .withOptionalSsl(sslContextName)
      .withConnectorPoolSize(connectionPoolSize)
      .mountService(PactStubService.service(interactionManager, config.giveStrictMode), "/")
  }

  private val isAdminCall: Request[IO] => Boolean = request =>
    request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  private def service(
      interactionManager: IInteractionManager,
      strictMatching: Boolean
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter): HttpService[IO] =
    Kleisli[OptIO, Request[IO], Response[IO]](matchRequestWithResponse(interactionManager, strictMatching, _))

  private def matchRequestWithResponse(
      interactionManager: IInteractionManager,
      strictMatching: Boolean,
      req: Request[IO]
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter): OptIO[Response[IO]] = {
    val resp = if (isAdminCall(req)) {
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
            req.attemptAs[String].fold(_ => None, Option.apply).unsafeRunSync().getOrElse("")
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
          query = if (req.params.isEmpty) None else Option(req.multiParams.toList.flatMap { case (key, values) => values.map((key,_))}.map(p => p._1 + "=" + p._2).mkString("&")),
          path = Option(req.pathInfo),
          body = req.attemptAs[String].fold(_ => None, Option.apply).unsafeRunSync(),
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

    OptionT.liftF(resp)
  }
}

class PactServer extends IPactStubber {

  private var instance: Option[Server[IO]] = None

  private def blazeBuilder(
      scalaPactSettings: ScalaPactSettings,
      interactionManager: IInteractionManager,
      connectionPoolSize: Int,
      sslContextName: Option[String],
      port: Option[Int]
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter, sslContextMap: SslContextMap): BlazeBuilder[IO] =
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
    scalaPactSettings => {
      instance match {
        case Some(_) =>
          this

        case None =>
          instance = Option(
            blazeBuilder(scalaPactSettings, interactionManager, connectionPoolSize, sslContextName, port).start
              .unsafeRunSync()
          )
          this
      }
    }

  def shutdown(): Unit = {
    instance.foreach(_.shutdown.unsafeRunSync())
    instance = None
  }

  def port: Option[Int] = instance.map(_.address.getPort)
}
