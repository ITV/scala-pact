package com.itv.scalapact.shared

final case class SimpleResponse(statusCode: Int, headers: Map[String, String], body: Option[String]) {
  def is2xx: Boolean = statusCode >= 200 && statusCode < 300

  def is3xx: Boolean = statusCode >= 300 && statusCode < 400

  def is4xx: Boolean = statusCode >= 400 && statusCode < 500

  def is5xx: Boolean = statusCode >= 500 && statusCode < 600
}

object SimpleResponse {

  def apply(status: Int): SimpleResponse =
    SimpleResponse(200, Map.empty[String, String], None)

}

final case class SimpleRequest(baseUrl: String, endPoint: String, method: HttpMethod, headers: Map[String, String], body: Option[String])

object SimpleRequest {

  def apply(baseUrl: String, endPoint: String, method: HttpMethod): SimpleRequest =
    SimpleRequest(baseUrl, endPoint, method, Map.empty[String, String], None)

  def apply(baseUrl: String, endPoint: String, method: HttpMethod, body: String): SimpleRequest =
    SimpleRequest(baseUrl, endPoint, method, Map.empty[String, String], Option(body))

}