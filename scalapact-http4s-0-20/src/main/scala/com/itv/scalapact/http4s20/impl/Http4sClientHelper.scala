package com.itv.scalapact.http4s20.impl

import cats.effect.{ContextShift, IO, Resource}
import com.itv.scalapact.shared.{SimpleRequest, SimpleResponse, SslContextMap}
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.headers.{AgentProduct, `User-Agent`}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Http4sClientHelper {
  def defaultClient: Resource[IO, Client[IO]] =
    buildPooledBlazeHttpClient(1, Duration(1, SECONDS), None)

  def buildPooledBlazeHttpClient(maxTotalConnections: Int,
                                 clientTimeout: Duration,
                                 sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Resource[IO, Client[IO]] = {
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    val sslContext = sslContextMap(sslContextName)
    val builder = BlazeClientBuilder[IO](ExecutionContext.Implicits.global)
      .withMaxTotalConnections(maxTotalConnections)
      .withRequestTimeout(clientTimeout)
      .withUserAgentOption(Option(`User-Agent`(AgentProduct("scala-pact", Option(BuildInfo.version)))))
    sslContext.fold(builder)(s => builder.withSslContext(s)).resource
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def doRequest(request: SimpleRequest, httpClient: Resource[IO, Client[IO]]): IO[SimpleResponse] =
    for {
      request <- Http4sRequestResponseFactory.buildRequest(request)
      response <- httpClient.use { c =>
        c.fetch[SimpleResponse](request) { r: Response[IO] =>
          r.bodyAsText.compile.toVector
            .map(_.mkString)
            .map { b =>
              SimpleResponse(r.status.code, r.headers.toMap, Some(b))
            }
        }
      }
    } yield response
}
