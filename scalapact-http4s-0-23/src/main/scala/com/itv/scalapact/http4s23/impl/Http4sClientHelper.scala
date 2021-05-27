package com.itv.scalapact.http4s23.impl

import cats.effect.{IO, Resource}
import com.itv.scalapact.shared.http.{SimpleRequest, SimpleResponse, SslContextMap}
import com.itv.scalapact.shared.utils.PactLogger
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.headers.`User-Agent`

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Http4sClientHelper {

  def defaultClient: Resource[IO, Client[IO]] =
    buildPooledBlazeHttpClient(1, Duration(5, SECONDS), None)

  def buildPooledBlazeHttpClient(maxTotalConnections: Int, clientTimeout: Duration, sslContextName: Option[String])(
      implicit sslContextMap: SslContextMap
  ): Resource[IO, Client[IO]] = {
    val sslContext                    = sslContextMap(sslContextName)
    val builder = BlazeClientBuilder[IO](ExecutionContext.Implicits.global)
      .withMaxTotalConnections(maxTotalConnections)
      .withRequestTimeout(clientTimeout)
      .withUserAgentOption(Option(`User-Agent`(ProductId("scala-pact", Option(BuildInfo.version)))))

    PactLogger.debug(
      s"Creating http4s client: connections $maxTotalConnections, timeout $clientTimeout, sslContextName: $sslContextName, sslContextMap: $sslContextMap"
    )

    sslContext.fold(builder)(s => builder.withSslContext(s)).resource
  }

  def doRequest(request: SimpleRequest, httpClient: Resource[IO, Client[IO]]): IO[SimpleResponse] =
    for {
      request <- Http4sRequestResponseFactory.buildRequest(request)
      _       <- IO(PactLogger.message(s"cURL for request: ${request.asCurl()}"))
      response <- httpClient.use { c =>
        c.run(request).use { r: Response[IO] =>
          r.bodyText.compile.toVector
            .map(_.mkString)
            .map { b =>
              SimpleResponse(r.status.code, r.headers.toMap, Some(b))
            }
        }
      }
    } yield response
}
