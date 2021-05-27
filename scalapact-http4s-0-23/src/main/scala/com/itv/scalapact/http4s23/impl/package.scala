package com.itv.scalapact.http4s23

import org.http4s.{Header, Headers}
import org.typelevel.ci.CIString

package object impl {
  implicit class HeaderOps(headers: Headers) {
    def toMap: Map[String, String] =
      headers.headers.map(h => h.name.toString -> h.value).toMap
  }

  implicit class MapOps(val values: Map[String, String]) extends AnyVal {
    def toHttp4sHeaders: Headers = Headers(values.map { case (k, v) => Header.Raw(CIString(k), v) }.toList)
  }

}
