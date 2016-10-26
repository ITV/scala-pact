package com.itv.scalapactcore.common

import java.nio.charset.StandardCharsets

import org.http4s._
import scodec.bits.ByteVector

import scala.language.implicitConversions
import scalaz.concurrent.Task

object Http4sRequestResponseFactory {

  import HeaderImplicitConversions._

  private val stringToByteVector: String => ByteVector = str => ByteVector(str.getBytes(StandardCharsets.UTF_8))

  def buildRequest(method: Method, uri: Uri, headers: Map[String, String], body: Option[String]): Task[Request] = {
    val request = Request(
      method = method,
      uri = uri,
      httpVersion = HttpVersion.`HTTP/1.1`,
      headers = headers,
      body = EmptyBody,
      attributes = AttributeMap.empty
    )

    body.map { b =>
      request.withBody(b)(EntityEncoder.simple()(stringToByteVector))
    }.getOrElse(Task(request))
  }

  def buildResponse(status: Status, headers: Map[String, String], body: Option[String]): Task[Response] = {
    val response = Response(
      status = status,
      httpVersion = HttpVersion.`HTTP/1.1`,
      headers = headers,
      body = EmptyBody,
      attributes = AttributeMap.empty
    )

    body.map { b =>
      response.withBody(b)(EntityEncoder.simple()(stringToByteVector))
    }.getOrElse(Task(response))
  }
}

object HeaderImplicitConversions {
  implicit def mapToHeaderList(headers: Map[String, String]): Headers = Headers(headers.toList.map(t => Header(t._1, t._2)))

  implicit def mapToHeaderList(headers: Headers): Map[String, String] =
    headers.toList.map(h => Header.unapply(h)).collect { case Some(h) => (h._1.toString, h._2) }.toMap
}