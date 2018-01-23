package com.itv.scalapact.shared.http

import java.util.concurrent.Executors
import javax.net.ssl.SSLContext

import cats.effect.IO
import com.itv.scalapact.shared.{SimpleRequest, SimpleResponse}
import org.http4s.client.Client
import org.http4s.client.blaze.{BlazeClientConfig, PooledHttp1Client}
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Http4sClientHelper {

  import HeaderImplicitConversions._
  import com.itv.scalapact.shared.RightBiasEither._

  private[http] implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  private def blazeClientConfig(clientTimeout: Duration, sslContext: Option[SSLContext]): BlazeClientConfig = BlazeClientConfig.defaultConfig.copy(
    requestTimeout = clientTimeout,
    sslContext = sslContext,
    userAgent = Option(`User-Agent`(AgentProduct("scala-pact", Option(BuildInfo.version)))),
    checkEndpointIdentification = false
  )

  private val extractResponse: Response[IO] => IO[SimpleResponse] = r =>
    r.bodyAsText.runLog.map(_.mkString).map { b => SimpleResponse(r.status.code, r.headers, Some(b)) }

  def defaultClient: Client[IO] =
    buildPooledBlazeHttpClient(1, Duration(1, SECONDS), None)

  def buildPooledBlazeHttpClient(maxTotalConnections: Int, clientTimeout: Duration, sslContext: Option[SSLContext]): Client[IO] =
    PooledHttp1Client[IO](maxTotalConnections = maxTotalConnections, config = blazeClientConfig(clientTimeout, sslContext))

  val doRequest: (SimpleRequest, Client[IO]) => IO[SimpleResponse] = (request, httpClient) =>
    for {
      request  <- Http4sRequestResponseFactory.buildRequest(request)
      response <- httpClient.fetch[SimpleResponse](request)(extractResponse)
      _        <- httpClient.shutdown
    } yield response

}
