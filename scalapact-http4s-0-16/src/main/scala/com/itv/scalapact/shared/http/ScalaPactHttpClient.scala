package com.itv.scalapact.shared.http

import com.itv.scalapact.shared._
import org.http4s.client.Client

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.{implicitConversions, postfixOps}
import scalaz.concurrent.Task

object ScalaPactHttpClient extends IScalaPactHttpClient {

  implicit def taskToFuture[A](x: => Task[A]): Future[A] = {
    import scalaz.{-\/, \/-}
    val p: Promise[A] = Promise()

    x.attemptRun match {
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
    doInteractionRequestTask(Http4sClientHelper.doRequest, url, ir, clientTimeout, sslContextName)

  def doRequestSync(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Either[Throwable, SimpleResponse] =
    doRequestTask(Http4sClientHelper.doRequest, simpleRequest).attemptRun.toEither

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse] =
    doInteractionRequestTask(Http4sClientHelper.doRequest, url, ir, clientTimeout,sslContextName).attemptRun.toEither

  def doRequestTask(performRequest: (SimpleRequest, Client) => Task[SimpleResponse], simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Task[SimpleResponse] =
    performRequest(simpleRequest, Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds))

  def doInteractionRequestTask(performRequest: (SimpleRequest, Client) => Task[SimpleResponse], url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Task[InteractionResponse] =
    performRequest(
      SimpleRequest( url, ir.path.getOrElse("") + ir.query.map(q => s"?$q").getOrElse(""), HttpMethod.maybeMethodToMethod(ir.method), ir.headers.getOrElse(Map.empty[String, String]), ir.body, sslContextName),
      Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, clientTimeout)
    ).map { r =>
      InteractionResponse(
        status = Option(r.statusCode),
        headers = if(r.headers.isEmpty) None else Option(r.headers.map(p => p._1 -> p._2.mkString)),
        body = r.body,
        matchingRules = None
      )
    }

}
