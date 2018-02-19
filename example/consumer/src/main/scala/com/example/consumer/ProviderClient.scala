package com.example.consumer

import org.json4s.DefaultFormats
import org.json4s.native.JsonParser._

import scalaj.http.{Http, HttpResponse}

object ProviderClient {

  private implicit val formats = DefaultFormats

  def fetchResults(baseUrl: String): Option[Results] = {
    Http(baseUrl + "/results").asString match {
      case r: HttpResponse[String] if r.is2xx =>
        parse(r.body).extractOpt[Results]

      case _ =>
        None
    }
  }

  def fetchAuthToken(host: String, port: Int, name: String): Option[Token] = {
    Http("http://" + host + ":" + port + "/auth_token?name=" + name)
      .headers(("Accept", "application/json"), ("Name", name))
      .asString match {
        case r: HttpResponse[String] if r.is2xx =>
          println(">> " + r)
          parse(r.body).extractOpt[Token]

        case r: HttpResponse[String] =>
          println("<< " + r)
          None
      }
  }

}

case class Results(count: Int, results: List[String])

case class Token(token: String)
