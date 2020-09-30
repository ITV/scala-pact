package com.itv.scalapact.http4s16a

import org.http4s.{Header, Headers}

package object impl {
  implicit class HeaderOps(headers: Headers) {
    def asMap: Map[String, String] =
      headers.toList.map { h => h.name.toString -> h.value }.toMap
  }

  implicit class MapOps(val values: Map[String, String]) extends AnyVal {
    def toHttp4sHeaders: Headers = Headers(values.map { case (k, v) => Header(k, v) }.toList)
  }

}
