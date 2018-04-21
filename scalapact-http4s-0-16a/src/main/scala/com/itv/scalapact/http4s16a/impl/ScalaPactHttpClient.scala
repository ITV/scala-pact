package com.itv.scalapact.http4s16a.impl

import com.itv.scalapact.shared._
import com.itv.scalapact.shared.typeclasses.IScalaPactHttpClient
import org.http4s.client.Client
import scalaz.concurrent.Task

import scala.concurrent.duration._

class ScalaPactHttpClient(fetcher: (SimpleRequest, Client) => Task[SimpleResponse]) extends IScalaPactHttpClient[Task] {

  val maxTotalConnections: Int = 1

  def doRequest(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Task[SimpleResponse] =
    doRequestTask(fetcher, simpleRequest)

  def doInteractionRequest(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): Task[InteractionResponse] =
    doInteractionRequestTask(fetcher, url, ir, clientTimeout, sslContextName)

  def doRequestSync(
      simpleRequest: SimpleRequest
  )(implicit sslContextMap: SslContextMap): Either[Throwable, SimpleResponse] =
    doRequestTask(fetcher, simpleRequest).unsafePerformSyncAttempt.toEither

  def doInteractionRequestSync(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse] =
    doInteractionRequestTask(fetcher, url, ir, clientTimeout, sslContextName).unsafePerformSyncAttempt.toEither

  def doRequestTask(performRequest: (SimpleRequest, Client) => Task[SimpleResponse],
                    simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Task[SimpleResponse] =
    SslContextMap(simpleRequest)(
      sslContext =>
        simpleRequestWithoutFakeHeader =>
          performRequest(simpleRequestWithoutFakeHeader,
                         Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds, sslContext))
    )

  def doInteractionRequestTask(
      performRequest: (SimpleRequest, Client) => Task[SimpleResponse],
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): Task[InteractionResponse] =
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
        simpleRequestWithoutFakeHeader,
        Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, clientTimeout, sslContext)
      ).map { r =>
        InteractionResponse(
          status = Option(r.statusCode),
          headers = if (r.headers.isEmpty) None else Option(r.headers.map(p => p._1 -> p._2.mkString)),
          body = r.body,
          matchingRules = None
        )
      }
    }

}
