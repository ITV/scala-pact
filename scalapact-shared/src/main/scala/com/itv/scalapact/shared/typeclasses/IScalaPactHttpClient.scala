package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared._

import scala.concurrent.duration.Duration

trait IScalaPactHttpClient[F[_]] {
  def doRequest(simpleRequest: SimpleRequest): F[SimpleResponse]

  def doInteractionRequest(url: String, ir: InteractionRequest): F[InteractionResponse]

  def doRequestSync(simpleRequest: SimpleRequest): Either[Throwable, SimpleResponse]

  def doInteractionRequestSync(url: String, ir: InteractionRequest): Either[Throwable, InteractionResponse]
}
