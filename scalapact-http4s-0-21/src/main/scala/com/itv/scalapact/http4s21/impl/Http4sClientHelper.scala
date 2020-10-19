package com.itv.scalapact.http4s21.impl

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

  def defaultClient: Resource[IO, Client[IO]] =
    buildPooledBlazeHttpClient(1, Duration(5, SECONDS), None)

  def buildPooledBlazeHttpClient(maxTotalConnections: Int,
                                 clientTimeout: Duration,
                                 sslContext: Option[SSLContext]): Resource[IO, Client[IO]] = {
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

    BlazeClientBuilder[IO](ExecutionContext.global)
      .withMaxTotalConnections(maxTotalConnections)
      .withRequestTimeout(clientTimeout)
      .withSslContext(sslContext.getOrElse(SSLContext.getDefault))
      .withUserAgentOption(Option(`User-Agent`(AgentProduct("scala-pact", Option(BuildInfo.version)))))
      .withCheckEndpointAuthentication(false)
      .resource
  }

  def doRequest(request: SimpleRequest, httpClient: Resource[IO, Client[IO]]): IO[SimpleResponse] =
    for {
      request <- Http4sRequestResponseFactory.buildRequest(request)
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
