package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared._

trait IScalaPactHttpClient {
  def doRequest(simpleRequest: SimpleRequest): Either[Throwable, SimpleResponse]
  def doInteractionRequest(url: String, ir: InteractionRequest): Either[Throwable, InteractionResponse]
}
