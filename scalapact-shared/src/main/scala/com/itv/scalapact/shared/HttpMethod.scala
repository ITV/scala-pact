package com.itv.scalapact.shared

sealed trait HttpMethod

object HttpMethod {

  def apply(method: String): Option[HttpMethod] =
    method match {
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

      case _ =>
        None
    }

  val maybeMethodToMethod: Option[String] => HttpMethod = maybeMethod =>
    maybeMethod.map(_.toUpperCase).flatMap(HttpMethod.apply).getOrElse(GET)

  case object GET     extends HttpMethod
  case object POST    extends HttpMethod
  case object PUT     extends HttpMethod
  case object DELETE  extends HttpMethod
  case object OPTIONS extends HttpMethod

}
