package com.itv.scalapact.shared.http

sealed abstract class HttpMethod(val name: String)

object HttpMethod {
  case object GET     extends HttpMethod("GET")
  case object POST    extends HttpMethod("POST")
  case object PUT     extends HttpMethod("PUT")
  case object DELETE  extends HttpMethod("DELETE")
  case object OPTIONS extends HttpMethod("OPTIONS")
  case object PATCH   extends HttpMethod("PATCH")
  case object CONNECT extends HttpMethod("CONNECT")
  case object TRACE   extends HttpMethod("TRACE")
  case object HEAD    extends HttpMethod("HEAD")

  def apply(method: String): Option[HttpMethod] =
    method.toUpperCase match {
      case "GET" =>
        Option(GET)

      case "POST" =>
        Option(POST)

      case "PUT" =>
        Option(PUT)

      case "DELETE" =>
        Option(DELETE)

      case "OPTIONS" =>
        Option(OPTIONS)

      case "PATCH" =>
        Option(PATCH)

      case "CONNECT" =>
        Option(CONNECT)

      case "TRACE" =>
        Option(TRACE)

      case "HEAD" =>
        Option(HEAD)

      case _ =>
        None
    }

  val maybeMethodToMethod: Option[String] => HttpMethod = maybeMethod =>
    maybeMethod.flatMap(HttpMethod.apply).getOrElse(GET)
}
