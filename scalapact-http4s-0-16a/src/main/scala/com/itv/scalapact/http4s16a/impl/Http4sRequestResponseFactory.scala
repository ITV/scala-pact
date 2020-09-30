package com.itv.scalapact.http4s16a.impl

import java.nio.charset.StandardCharsets

import com.itv.scalapact.shared.HttpMethod._
import com.itv.scalapact.shared.{HttpMethod, SimpleRequest}
import org.http4s._
import scalaz.concurrent.Task
import scodec.bits.ByteVector

object Http4sRequestResponseFactory {

  val stringToByteVector: String => ByteVector = str => ByteVector(str.getBytes(StandardCharsets.UTF_8))

  def buildUri(baseUrl: String, endpoint: String): Task[Uri] =
    Task.fromDisjunction(Uri.fromString(baseUrl + endpoint).leftMap(l => new Exception(l.message)))

  def intToStatus(status: IntAndReason): Task[Status] =
    status match {
      case IntAndReason(code, Some(reason)) =>
        Task.fromDisjunction(Status.fromIntAndReason(code, reason))

      case IntAndReason(code, None) =>
        Task.fromDisjunction(Status.fromInt(code))
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

      case PATCH =>
        Method.PATCH

      case CONNECT =>
        Method.CONNECT

      case TRACE =>
        Method.TRACE

      case HEAD =>
        Method.HEAD
    }

  def buildRequest(request: SimpleRequest): Task[Request] =
    buildUri(request.baseUrl, request.endPoint).flatMap { uri =>
      val r = Request(
        method = httpMethodToMethod(request.method),
        uri = uri,
        httpVersion = HttpVersion.`HTTP/1.1`,
        headers = request.headers.toHttp4sHeaders,
        body = EmptyBody,
        attributes = AttributeMap.empty
      )

      request.body
        .map { b =>
          r.withBody(b)(EntityEncoder.simple()(stringToByteVector))
        }
        .getOrElse(Task(r))
    }

  def buildResponse(status: IntAndReason, headers: Map[String, String], body: Option[String]): Task[Response] =
    intToStatus(status).flatMap { code =>
      val response = Response(
        status = code,
        httpVersion = HttpVersion.`HTTP/1.1`,
        headers = headers.toHttp4sHeaders,
        body = EmptyBody,
        attributes = AttributeMap.empty
      )

      body
        .map { b =>
          response.withBody(b)(EntityEncoder.simple()(stringToByteVector))
        }
        .getOrElse(Task(response))
    }

}

case class IntAndReason(code: Int, reason: Option[String])
