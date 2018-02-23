package com.itv.scalapact.shared.http

import com.itv.scalapact.shared.{SimpleRequest, _}

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
import scalaz.concurrent.Task

object ScalaPactHttpClient extends IScalaPactHttpClient[Task] {

  private val maxTotalConnections: Int = 1

  implicit val caller: Caller = Http4sClientHelper.doRequest(Http4sClientHelper.buildPooledBlazeHttpClient(maxTotalConnections, 2.seconds))

  def doRequest(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap, caller: Caller): Task[SimpleResponse] =
    caller(simpleRequest)

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap, caller: Caller): Task[InteractionResponse] =
    caller(
      SimpleRequest(url, ir.path.getOrElse("") + ir.query.map(q => s"?$q").getOrElse(""), HttpMethod.maybeMethodToMethod(ir.method), ir.headers.getOrElse(Map.empty[String, String]), ir.body, sslContextName),
    ).map { r =>
      InteractionResponse(
        status = Option(r.statusCode),
        headers = if(r.headers.isEmpty) None else Option(r.headers.map(p => p._1 -> p._2.mkString)),
        body = r.body,
        matchingRules = None
      )
    }

  def doRequestSync(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap, caller: Caller): Either[Throwable, SimpleResponse] =
    doRequest(simpleRequest).attemptRun.toEither

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap, caller: Caller): Either[Throwable, InteractionResponse] =
    doInteractionRequest(url, ir, clientTimeout,sslContextName).attemptRun.toEither

}
