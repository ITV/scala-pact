package com.itv.scalapact.shared.http

import com.itv.scalapact.shared._
import org.http4s.client.Client

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions
import scalaz.concurrent.Task
import com.itv.scalapact.shared.PactLogger


object ScalaPactHttpClient extends IScalaPactHttpClient {

  implicit def taskToFuture[A](x: => Task[A]): Future[A] = {
    import scalaz.{-\/, \/-}
    val p: Promise[A] = Promise()

    x.unsafePerformAsync {
      case -\/(ex) =>
        p.failure(ex)
        ()

      case \/-(r) =>
        p.success(r)
        ()
    }
    p.future
  }

  val maxTotalConnections: Int = 1

  def doRequest(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Future[SimpleResponse] =
    doRequestTask(Http4sClientHelper.doRequest, simpleRequest)

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Future[InteractionResponse] =
    doInteractionRequestTask(Http4sClientHelper.doRequest, url, ir, clientTimeout,sslContextName)

  def doRequestSync(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Either[Throwable, SimpleResponse] =
    doRequestTask(Http4sClientHelper.doRequest, simpleRequest).unsafePerformSyncAttempt.toEither

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse] =
    doInteractionRequestTask(Http4sClientHelper.doRequest, url, ir, clientTimeout,sslContextName).unsafePerformSyncAttempt.toEither

  def doRequestTask(performRequest: (SimpleRequest, Client) => Task[SimpleResponse], simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Task[SimpleResponse] =
    SslContextMap(simpleRequest) { sslContext =>
      simpleRequestWithoutFakeheader =>
        PactLogger.debug(s"doRequestTask${simpleRequest} ==> ${simpleRequestWithoutFakeheader}")
        performRequest(simpleRequestWithoutFakeheader, Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds, sslContext))
    }

  def doInteractionRequestTask(performRequest: (SimpleRequest, Client) => Task[SimpleResponse], url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Task[InteractionResponse] =
    SslContextMap(SimpleRequest(url, ir.path.getOrElse("") + ir.query.map(q => s"?$q").getOrElse(""), HttpMethod.maybeMethodToMethod(ir.method), ir.headers.getOrElse(Map.empty[String, String]), ir.body, sslContextName)) {
      sslContext =>
        simpleRequestWithoutFakeheader =>
          PactLogger.debug(s"doInteractionRequestTask  ${simpleRequestWithoutFakeheader}")
          performRequest(simpleRequestWithoutFakeheader,
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
