package com.example

import scalaj.http.{Http, HttpResponse}
import org.json4s.DefaultFormats
import org.json4s.native.JsonParser._

object ProviderClient {

  private implicit val formats = DefaultFormats

  val fetchResults: String => Option[Results] = baseUrl =>
    Http(baseUrl + "/results").asString match {
      case r: HttpResponse[String] if r.is2xx =>
        parse(r.body).extractOpt[Results]

      case _ =>
        None
    }

}

case class Results(count: Int, result: List[String])
