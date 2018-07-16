package com.itv.scalapact.http4s18.impl

import cats.effect.IO
import com.itv.scalapact.shared.{SimpleRequest, SimpleResponse}
import javax.net.ssl.SSLContext
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.{BlazeClientConfig, Http1Client}
import org.http4s.headers.{AgentProduct, `User-Agent`}

import scala.concurrent.duration._

object Http4sClientHelper {

  import HeaderImplicitConversions._

  private def blazeClientConfig(clientTimeout: Duration,
                                sslContext: Option[SSLContext],
                                maxTotalConnections: Int): BlazeClientConfig =
    BlazeClientConfig.defaultConfig.copy(
      requestTimeout = clientTimeout,
      sslContext = sslContext,
      userAgent = Option(`User-Agent`(AgentProduct("scala-pact", Option(BuildInfo.version)))),
      checkEndpointIdentification = false,
      maxTotalConnections = maxTotalConnections
    )

  private val extractResponse: Response[IO] => IO[SimpleResponse] = r =>
    r.bodyAsText.compile.toVector.map(_.mkString).map { b =>
      SimpleResponse(r.status.code, r.headers, Some(b))
  }

  def defaultClient: IO[Client[IO]] =
    buildPooledBlazeHttpClient(1, Duration(1, SECONDS), None)

  def buildPooledBlazeHttpClient(maxTotalConnections: Int,
                                 clientTimeout: Duration,
                                 sslContext: Option[SSLContext]): IO[Client[IO]] =
    Http1Client[IO](config = blazeClientConfig(clientTimeout, sslContext, maxTotalConnections))

  def doRequest(request: SimpleRequest, httpClient: IO[Client[IO]]): IO[SimpleResponse] =
    for {
      request  <- Http4sRequestResponseFactory.buildRequest(request)
      client   <- httpClient
      response <- client.fetch[SimpleResponse](request)(extractResponse)
      _        <- client.shutdown
    } yield response

}
