package com.itv.scalapact.http4s17.impl

import com.itv.scalapact.shared.typeclasses.IScalaPactHttpClient
import com.itv.scalapact.shared.{SimpleRequest, _}
import fs2.Task
import org.http4s.client.Client

import scala.concurrent.duration._

class ScalaPactHttpClient(fetcher: (SimpleRequest, Client) => Task[SimpleResponse]) extends IScalaPactHttpClient[Task] {

  val maxTotalConnections: Int = 1

  def doRequest(simpleRequest: SimpleRequest, clientTimeout: Duration)(implicit sslContextMap: SslContextMap): Task[SimpleResponse] =
    doRequestTask(fetcher, simpleRequest, clientTimeout)

  def doInteractionRequest(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): Task[InteractionResponse] =
    doInteractionRequestTask(fetcher, url, ir, clientTimeout, sslContextName)

  def doRequestSync(
      simpleRequest: SimpleRequest, clientTimeout: Duration
  )(implicit sslContextMap: SslContextMap): Either[Throwable, SimpleResponse] =
    doRequestTask(fetcher, simpleRequest, clientTimeout).unsafeAttemptRun()

  def doInteractionRequestSync(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse] =
    doInteractionRequestTask(fetcher, url, ir, clientTimeout, sslContextName).unsafeAttemptRun()

  def doRequestTask(performRequest: (SimpleRequest, Client) => Task[SimpleResponse],
                    simpleRequest: SimpleRequest, clientTimeout: Duration)(implicit sslContextMap: SslContextMap): Task[SimpleResponse] =
    SslContextMap(simpleRequest) { sslContext => simpleRequestWithoutFakeHeader =>
      performRequest(simpleRequestWithoutFakeHeader,
                     Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, clientTimeout, sslContext))
    }

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
