package com.itv.scalapact.http4s18.impl

import cats.effect.IO
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.typeclasses.IScalaPactHttpClient
import org.http4s.client.Client

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}

object ScalaPactHttpClient extends IScalaPactHttpClient {

  private val maxTotalConnections: Int = 1

  private implicit val executionContext: ExecutionContext = Http4sClientHelper.executionContext

  def doRequest(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Future[SimpleResponse] =
    doRequestIO(Http4sClientHelper.doRequest, simpleRequest).unsafeToFuture()

  def doInteractionRequest(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Future[InteractionResponse] =
    doInteractionRequestIO(Http4sClientHelper.doRequest, url, ir, clientTimeout, sslContextName).unsafeToFuture()

  def doRequestSync(simpleRequest: SimpleRequest)(
      implicit sslContextMap: SslContextMap): Either[Throwable, SimpleResponse] =
    doRequestIO(Http4sClientHelper.doRequest, simpleRequest).attempt.unsafeRunSync()

  def doInteractionRequestSync(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse] =
    doInteractionRequestIO(Http4sClientHelper.doRequest, url, ir, clientTimeout, sslContextName).attempt.unsafeRunSync()

  def doRequestIO(performRequest: (SimpleRequest, Client[IO]) => IO[SimpleResponse], simpleRequest: SimpleRequest)(
      implicit sslContextMap: SslContextMap): IO[SimpleResponse] =
    SslContextMap(simpleRequest)(
      sslContext =>
        simpleRequestWithoutFakeheader =>
          performRequest(simpleRequestWithoutFakeheader,
                         Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds, sslContext)))

  def doInteractionRequestIO(performRequest: (SimpleRequest, Client[IO]) => IO[SimpleResponse],
                             url: String,
                             ir: InteractionRequest,
                             clientTimeout: Duration,
                             sslContextName: Option[String]): IO[InteractionResponse] =
    SslContextMap(
      SimpleRequest(
        url,
        ir.path.getOrElse("") + ir.query.map(q => s"?$q").getOrElse(""),
        HttpMethod.maybeMethodToMethod(ir.method),
        ir.headers.getOrElse(Map.empty[String, String]),
        ir.body,
        sslContextName
      )) { sslContext => simpleRequestWithoutFakeheader =>
      performRequest(
        simpleRequestWithoutFakeheader,
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
