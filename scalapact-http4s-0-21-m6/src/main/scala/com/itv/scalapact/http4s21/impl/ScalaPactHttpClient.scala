package com.itv.scalapact.http4s21.impl

import cats.effect._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.typeclasses.IScalaPactHttpClient
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ScalaPactHttpClient(fetcher: (SimpleRequest, Resource[IO, Client[IO]]) => IO[SimpleResponse])
    extends IScalaPactHttpClient[IO] {

  private val maxTotalConnections: Int = 1

  private implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def doRequest(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): IO[SimpleResponse] =
    doRequestIO(fetcher, simpleRequest)

  def doInteractionRequest(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): IO[InteractionResponse] =
    doInteractionRequestIO(fetcher, url, ir, clientTimeout, sslContextName)

  def doRequestSync(
      simpleRequest: SimpleRequest
  )(implicit sslContextMap: SslContextMap): Either[Throwable, SimpleResponse] =
    doRequestIO(fetcher, simpleRequest).attempt.unsafeRunSync()

  def doInteractionRequestSync(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse] =
    doInteractionRequestIO(fetcher, url, ir, clientTimeout, sslContextName).attempt
      .unsafeRunSync()

  def doRequestIO(performRequest: (SimpleRequest, Resource[IO, Client[IO]]) => IO[SimpleResponse],
                  simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): IO[SimpleResponse] =
    SslContextMap(simpleRequest)(
      sslContext =>
        simpleRequestWithoutFakeHeader =>
          performRequest(
            simpleRequestWithoutFakeHeader, {
              val client = BlazeClientBuilder[IO](ExecutionContext.Implicits.global)
                .withMaxTotalConnections(maxTotalConnections)
                .withRequestTimeout(2.seconds)
              sslContext.fold(client)(s => client.withSslContext(s)).resource
            }
      )
    )

  def doInteractionRequestIO(
      performRequest: (SimpleRequest, Resource[IO, Client[IO]]) => IO[SimpleResponse],
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): IO[InteractionResponse] =
    SslContextMap(
      SimpleRequest(
        url,
        ir.path.getOrElse("") + ir.query.map(q => s"?$q").getOrElse(""),
        HttpMethod.maybeMethodToMethod(ir.method),
        ir.headers.getOrElse(Map.empty[String, String]),
        ir.body,
        sslContextName
      )
    ) { sslContext => simpleRequestWithoutFakeHeader =>
      performRequest(
        simpleRequestWithoutFakeHeader, {
          val client = BlazeClientBuilder[IO](ExecutionContext.Implicits.global)
            .withMaxTotalConnections(maxTotalConnections)
            .withRequestTimeout(clientTimeout)
          sslContext.fold(client)(s => client.withSslContext(s)).resource
        }
      ).map { r =>
        InteractionResponse(
          status = Option(r.statusCode),
          headers =
            if (r.headers.isEmpty) None
            else Option(r.headers.map(p => p._1 -> p._2.mkString)),
          body = r.body,
          matchingRules = None
        )
      }
    }

}
