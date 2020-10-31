package com.itv.scalapact.http4s21.impl

import java.util.concurrent.Executors

import cats.data.OptionT
import cats.effect._
import cats.implicits._
import com.itv.scalapact.shared.http.SslContextMap
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.{ScalaPactSettings, _}
import javax.net.ssl.SSLContext
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.util.CaseInsensitiveString
import org.http4s.{BuildInfo => _, _}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PactStubService {

  implicit class BlazeBuilderOps(val blazeBuilder: BlazeServerBuilder[IO]) extends AnyVal {
    def withOptionalSsl(sslContext: Option[SSLContext]): BlazeServerBuilder[IO] =
      sslContext.fold(blazeBuilder)(ssl => blazeBuilder.withSslContext(ssl))
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

    BlazeServerBuilder[IO](executionContext)
      .bindHttp(port.getOrElse(config.givePort), config.giveHost)
      .withIdleTimeout(60.seconds)
      .withOptionalSsl(sslContextMap(sslContextName))
      .withConnectorPoolSize(connectionPoolSize)
      .withHttpApp(PactStubService.service(interactionManager, config.giveStrictMode))
  }

  def service(
      interactionManager: IInteractionManager,
      strictMatching: Boolean
  )(implicit pactReader: IPactReader, pactWriter: IPactWriter): HttpApp[IO] = {

    val isAdminCall: Request[IO] => Boolean = request =>
      request.headers.get(CaseInsensitiveString("X-Pact-Admin")).map(_.value).contains("true")

    def admin(routes: HttpRoutes[IO]): HttpRoutes[IO] = HttpRoutes { req =>
      if (isAdminCall(req)) routes(req)
      else OptionT.none
    }

    def statusRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> Root / "stub" / "status" =>
      Ok()
    }

    def interactionsRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case req @ POST -> Root / "interactions" => putOrPostAdminInteraction(req)
      case req @ PUT -> Root / "interactions"  => putOrPostAdminInteraction(req)
      case GET -> Root / "interactions" =>
        val output =
          pactWriter.pactToJsonString(
            Pact(PactActor(""), PactActor(""), interactionManager.getInteractions, None, None),
            BuildInfo.version
          )
        Ok(JsonString(output))
      case DELETE -> Root / "interactions" =>
        IO(interactionManager.clearInteractions()).flatMap { _ =>
          val output =
            pactWriter.pactToJsonString(
              Pact(PactActor(""), PactActor(""), interactionManager.getInteractions, None, None),
              BuildInfo.version
            )
          Ok(JsonString(output))
        }
    }

    def putOrPostAdminInteraction(req: Request[IO]): IO[Response[IO]] =
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
            Ok(JsonString(output))

          case Left(l) =>
            InternalServerError(l)
        }

    def pactRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] { case req =>
      req.attemptAs[String].toOption.value.flatMap { maybeBody =>
        interactionManager.findMatchingInteraction(
          InteractionRequest(
            method = Option(req.method.name.toUpperCase),
            headers = Option(req.headers.toMap),
            query =
              if (req.params.isEmpty) None
              else
                Option(
                  req.multiParams.toList
                    .flatMap { case (key, values) => values.map((key, _)) }
                    .map(p => p._1 + "=" + p._2)
                    .mkString("&")
                ),
            path = Option(req.pathInfo),
            body = maybeBody,
            matchingRules = None
          ),
          strictMatching = strictMatching
        ) match {
          case Right(ir) =>
            Status.fromInt(ir.response.status.getOrElse(200)) match {
              case Right(_) =>
                IO(
                  Http4sRequestResponseFactory.buildResponse(
                    status = IntAndReason(ir.response.status.getOrElse(200), None),
                    headers = ir.response.headers.getOrElse(Map.empty),
                    body = ir.response.body
                  )
                )

              case Left(l) =>
                InternalServerError(l.sanitized)
            }

          case Left(message) =>
            IO(
              Http4sRequestResponseFactory.buildResponse(
                status = IntAndReason(598, Some("Pact Match Failure")),
                headers = Map("X-Pact-Admin" -> "Pact Match Failure"),
                body = Option(message)
              )
            )
        }
      }
    }

    (admin(statusRoutes) <+> admin(interactionsRoutes) <+> pactRoutes).orNotFound
  }
}
