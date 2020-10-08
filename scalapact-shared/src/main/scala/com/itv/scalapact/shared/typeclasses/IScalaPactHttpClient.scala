package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared._

import scala.concurrent.duration.Duration

trait IScalaPactHttpClient[F[_]] {

  def doRequest(simpleRequest: SimpleRequest, clientTimeout: Duration)(implicit sslContextMap: SslContextMap): F[SimpleResponse]

  def doInteractionRequest(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): F[InteractionResponse]

  def doRequestSync(simpleRequest: SimpleRequest, clientTimeout: Duration)(
      implicit sslContextMap: SslContextMap
  ): Either[Throwable, SimpleResponse]

  def doInteractionRequestSync(
      url: String,
      ir: InteractionRequest,
      clientTimeout: Duration,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse]

  def fetchHALIndex: F[HALIndex]

  def fetchHALIndexSync: HALIndex
}
