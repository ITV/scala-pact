package com.itv.scalapact.http4s16a.impl

import com.itv.scalapact.shared.{SimpleRequest, SimpleResponse}
import javax.net.ssl.SSLContext
import org.http4s.client.Client
import org.http4s.client.blaze.{BlazeClientConfig, PooledHttp1Client}
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s.{BuildInfo, Response}
import scalaz.concurrent.Task

import scala.concurrent.duration._

object Http4sClientHelper {

  private def blazeClientConfig(clientTimeout: Duration, sslContext: Option[SSLContext]): BlazeClientConfig =
    BlazeClientConfig.defaultConfig.copy(
      requestTimeout = clientTimeout,
      userAgent = Option(`User-Agent`(AgentProduct("scala-pact", Option(BuildInfo.version)))),
      checkEndpointIdentification = false,
      sslContext = sslContext
    )

  private val extractResponse: Response => Task[SimpleResponse] = r =>
    r.bodyAsText.runLog[Task, String].map(_.mkString).map { b =>
      SimpleResponse(r.status.code, r.headers.asMap, Some(b))
  }

  def defaultClient: Client =
    buildPooledBlazeHttpClient(1, Duration(1, SECONDS), sslContext = None)

  def buildPooledBlazeHttpClient(maxTotalConnections: Int,
                                 clientTimeout: Duration,
                                 sslContext: Option[SSLContext]): Client =
    PooledHttp1Client(maxTotalConnections, blazeClientConfig(clientTimeout, sslContext))

  val doRequest: (SimpleRequest, Client) => Task[SimpleResponse] = (request, httpClient) =>
    for {
      request  <- Http4sRequestResponseFactory.buildRequest(request)
      response <- httpClient.fetch[SimpleResponse](request)(extractResponse)
      _        <- httpClient.shutdown
    } yield response

}
