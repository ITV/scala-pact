package com.itv.scalapact.shared.http

import com.itv.scalapact.shared.{InteractionRequest, InteractionResponse}

trait IScalaPactHttpClient {
  def doRequest(simpleRequest: SimpleRequest): Either[Throwable, SimpleResponse]

  def doInteractionRequest(url: String, ir: InteractionRequest): Either[Throwable, InteractionResponse]
}
