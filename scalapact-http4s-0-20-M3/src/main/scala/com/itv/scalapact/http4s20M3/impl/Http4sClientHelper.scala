package com.itv.scalapact.http4s20M3.impl

import cats.effect.{ContextShift, IO, Resource}
import com.itv.scalapact.shared.{SimpleRequest, SimpleResponse}
import javax.net.ssl.SSLContext
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.headers.{AgentProduct, `User-Agent`}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Http4sClientHelper {

  import HeaderImplicitConversions._

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private val extractResponse: Response[IO] => IO[SimpleResponse] = r =>
    r.bodyAsText.compile.toVector.map(_.mkString).map { b =>
      SimpleResponse(r.status.code, r.headers, Some(b))
  }

  def defaultClient: Resource[IO, Client[IO]] =
    buildPooledBlazeHttpClient(1, Duration(1, SECONDS), None)

  def buildPooledBlazeHttpClient(maxTotalConnections: Int,
                                 clientTimeout: Duration,
                                 sslContext: Option[SSLContext]): Resource[IO, Client[IO]] = {
    implicit val cs: ContextShift[IO] =  IO.contextShift(ExecutionContext.global)

    BlazeClientBuilder[IO](ExecutionContext.global)
      .withMaxTotalConnections(maxTotalConnections)
      .withRequestTimeout(clientTimeout)
      .withSslContextOption(sslContext)
      .withUserAgentOption(Option(`User-Agent`(AgentProduct("scala-pact", Option(BuildInfo.version)))))
      .withCheckEndpointAuthentication(false)
      .resource
  }

  def doRequest(request: SimpleRequest, httpClient: IO[Client[IO]]): IO[SimpleResponse] =
    for {
      request  <- Http4sRequestResponseFactory.buildRequest(request)
      client   <- httpClient
      response <- client.fetch[SimpleResponse](request)(extractResponse)
    } yield response

}
