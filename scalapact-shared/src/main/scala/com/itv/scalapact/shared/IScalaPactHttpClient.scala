package com.itv.scalapact.shared

import scala.concurrent.duration.Duration

trait IScalaPactHttpClient[F[_]] {

  type Caller = SimpleRequest => F[SimpleResponse]

  implicit val caller: Caller

  def doRequest(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap, caller: Caller): F[SimpleResponse]

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap, caller: Caller): F[InteractionResponse]

  def doRequestSync(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap, caller: Caller): Either[Throwable, SimpleResponse]

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap, caller: Caller): Either[Throwable, InteractionResponse]

}
