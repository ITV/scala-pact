package com.itv.scalapact.shared.http

final case class SimpleResponse(statusCode: Int, headers: Map[String, String], body: Option[String]) {
  def is2xx: Boolean = statusCode >= 200 && statusCode < 300

  def is3xx: Boolean = statusCode >= 300 && statusCode < 400

  def is4xx: Boolean = statusCode >= 400 && statusCode < 500

  def is5xx: Boolean = statusCode >= 500 && statusCode < 600
}

object SimpleResponse {
  def apply(status: Int): SimpleResponse =
    SimpleResponse(status, Map.empty[String, String], None)
}
