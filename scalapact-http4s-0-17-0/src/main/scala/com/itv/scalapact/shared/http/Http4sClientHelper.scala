package com.itv.scalapact.shared.http

import com.itv.scalapact.shared.{SimpleRequest, SimpleResponse}
import fs2.{Strategy, Task}
import org.http4s.client.Client
import org.http4s.client.blaze.{BlazeClientConfig, PooledHttp1Client}
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s._

import scala.concurrent.duration._

object Http4sClientHelper {

  import HeaderImplicitConversions._
  import com.itv.scalapact.shared.RightBiasEither._

  private[http] implicit val strategy: Strategy = fs2.Strategy.fromFixedDaemonPool(2, threadName = "strategy")

  private def blazeClientConfig(clientTimeout: Duration): BlazeClientConfig = BlazeClientConfig.defaultConfig.copy(
    requestTimeout = clientTimeout,
    userAgent = Option(`User-Agent`(AgentProduct("scala-pact", Option(BuildInfo.version)))),
    checkEndpointIdentification = false
  )

  private val extractResponse: Response => Task[SimpleResponse] = r =>
    r.bodyAsText.runLog.map(_.mkString).map { b => SimpleResponse(r.status.code, r.headers, Some(b)) }

  def defaultClient: Client =
    buildPooledBlazeHttpClient(1, Duration(1, SECONDS))

  def buildPooledBlazeHttpClient(maxTotalConnections: Int, clientTimeout: Duration): Client =
    PooledHttp1Client(maxTotalConnections, blazeClientConfig(clientTimeout))

  val doRequest: (SimpleRequest, Client) => Task[SimpleResponse] = (request, httpClient) =>
    for {
      request  <- Http4sRequestResponseFactory.buildRequest(request)
      response <- httpClient.fetch[SimpleResponse](request)(extractResponse)
      _        <- httpClient.shutdown
    } yield response

}
