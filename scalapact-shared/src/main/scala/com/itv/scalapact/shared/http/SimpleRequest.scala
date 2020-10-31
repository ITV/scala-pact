package com.itv.scalapact.shared.http

final case class SimpleRequest(
    baseUrl: String,
    endPoint: String,
    method: HttpMethod,
    headers: Map[String, String],
    body: Option[String],
    sslContextName: Option[String]
)

object SimpleRequest {

  def apply(baseUrl: String, endPoint: String, method: HttpMethod, sslContextName: Option[String]): SimpleRequest =
    SimpleRequest(baseUrl, endPoint, method, Map.empty[String, String], None, sslContextName)

  def apply(
      baseUrl: String,
      endPoint: String,
      method: HttpMethod,
      body: String,
      sslContextName: Option[String]
  ): SimpleRequest =
    SimpleRequest(baseUrl, endPoint, method, Map.empty[String, String], Option(body), sslContextName)

}
