package com.itv.scalapact.shared

import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait IScalaPactHttpClient {

  def doRequest(simpleRequest: SimpleRequest): Future[SimpleResponse]

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration): Future[InteractionResponse]

  def doRequestSync(simpleRequest: SimpleRequest): Either[Throwable, SimpleResponse]

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration): Either[Throwable, InteractionResponse]
}
