package com.itv.scalapact.shared.http

import java.nio.charset.StandardCharsets

import com.itv.scalapact.shared.{HttpMethod, SimpleRequest}
import com.itv.scalapact.shared.HttpMethod._
import fs2.{Chunk, Strategy, Task}
import org.http4s._
import scodec.bits.ByteVector

import scala.language.implicitConversions

object Http4sRequestResponseFactory {

  import HeaderImplicitConversions._
  import com.itv.scalapact.shared.RightBiasEither._

  implicit def toTask[A <: Throwable, B](a: Either[A, B]): Task[B] =
    Task.fromAttempt(a)

  private val stringToByteVector: String => Chunk[Byte] = str => {
    Chunk.bytes(ByteVector(str.getBytes(StandardCharsets.UTF_8)).toArray)
  }

  def buildUri(baseUrl: String, endpoint: String): Task[Uri] =
    Uri.fromString(baseUrl + endpoint).leftMap(l => new Exception(l.message))

  def intToStatus(status: IntAndReason): Task[Status] =
    status match {
      case IntAndReason(code, Some(reason)) =>
        Status.fromIntAndReason(code, reason)

      case IntAndReason(code, None) =>
        Status.fromInt(code)
    }

  def httpMethodToMethod(httpMethod: HttpMethod): Method =
    httpMethod match {
      case GET =>
        Method.GET

      case POST =>
        Method.POST

      case PUT =>
        Method.PUT

      case DELETE =>
        Method.DELETE

      case OPTIONS =>
        Method.OPTIONS
    }

  def buildRequest(request: SimpleRequest)(implicit strategy: Strategy): Task[Request] =
    buildUri(request.baseUrl, request.endPoint).flatMap { uri =>
      val r = Request(
        method = httpMethodToMethod(request.method),
        uri = uri,
        httpVersion = HttpVersion.`HTTP/1.1`,
        headers = request.headers,
        body = EmptyBody,
        attributes = AttributeMap.empty
      )

      request.body.map { b =>
        r.withBody(b)(EntityEncoder.simple()(stringToByteVector))
      }.getOrElse(Task(r))
    }

  def buildResponse(status: IntAndReason, headers: Map[String, String], body: Option[String])(implicit strategy: Strategy): Task[Response] =
    intToStatus(status).flatMap { code =>
      val response = Response(
        status = code,
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

case class IntAndReason(code: Int, reason: Option[String])
