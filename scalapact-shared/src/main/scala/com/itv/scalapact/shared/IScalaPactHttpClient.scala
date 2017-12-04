package com.itv.scalapact.shared

import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait IScalaPactHttpClient {

  def doRequest(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Future[SimpleResponse]

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Future[InteractionResponse]

  def doRequestSync(simpleRequest: SimpleRequest)(implicit sslContextMap: SslContextMap): Either[Throwable, SimpleResponse]

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse]
}
