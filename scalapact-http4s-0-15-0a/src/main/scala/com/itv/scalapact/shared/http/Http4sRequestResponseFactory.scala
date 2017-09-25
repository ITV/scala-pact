package com.itv.scalapact.shared.http

import java.nio.charset.StandardCharsets

import com.itv.scalapact.shared.HttpMethod
import com.itv.scalapact.shared.HttpMethod._
import org.http4s._
import scodec.bits.ByteVector

import scala.language.implicitConversions
import scalaz.\/
import scalaz.concurrent.Task

object Http4sRequestResponseFactory {

  import HeaderImplicitConversions._

  val stringToByteVector: String => ByteVector = str => ByteVector(str.getBytes(StandardCharsets.UTF_8))

  implicit def toTask[A <: Throwable, B](a: \/[A, B]): Task[B] =
    Task.fromDisjunction(a)

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

  def buildRequest(httpMethod: HttpMethod, baseUrl: String, endPoint: String, headers: Map[String, String], body: Option[String]): Task[Request] =
    buildUri(baseUrl, endPoint).flatMap { uri =>
      val request = Request(
        method = httpMethodToMethod(httpMethod),
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


  def buildResponse(status: IntAndReason, headers: Map[String, String], body: Option[String]): Task[Response] =
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
  implicit def mapToHeaderList(headers: Map[String, String]): Headers =
    Headers(headers.toList.map(t => Header(t._1, t._2)))

  implicit def mapToHeaderList(headers: Headers): Map[String, String] =
    headers.toList.map(h => Header.unapply(h)).collect { case Some(h) => (h._1.toString, h._2) }.toMap
}

case class IntAndReason(code: Int, reason: Option[String])