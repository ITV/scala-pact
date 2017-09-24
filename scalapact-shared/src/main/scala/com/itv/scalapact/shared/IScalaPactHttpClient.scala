package com.itv.scalapact.shared

import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait IScalaPactHttpClient {

  def doRequest(method: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Future[SimpleResponse]

  def doInteractionRequest(url: String, ir: InteractionRequest, clientTimeout: Duration): Future[InteractionResponse]

  def doRequestSync(method: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Either[Throwable, SimpleResponse]

  def doInteractionRequestSync(url: String, ir: InteractionRequest, clientTimeout: Duration): Either[Throwable, InteractionResponse]
}
