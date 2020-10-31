package com.itv.scalapact.http4s21.impl

import java.nio.charset.StandardCharsets

import cats.effect.IO
import com.itv.scalapact.shared.http.HttpMethod._
import com.itv.scalapact.shared.http.{HttpMethod, SimpleRequest}
import fs2.Chunk
import org.http4s.Method
import org.http4s._
import scodec.bits.ByteVector

object Http4sRequestResponseFactory {

  implicit val enc: EntityEncoder[IO, String] =
    EntityEncoder.simple[IO, String]() { str =>
      Chunk.bytes(ByteVector(str.getBytes(StandardCharsets.UTF_8)).toArray)
    }

  def buildUri(baseUrl: String, endpoint: String): IO[Uri] =
    IO.fromEither(Uri.fromString(baseUrl + endpoint))

  def intToStatus(status: IntAndReason): ParseResult[Status] =
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

      case PATCH =>
        Method.PATCH

      case CONNECT =>
        Method.CONNECT

      case TRACE =>
        Method.TRACE

      case HEAD =>
        Method.HEAD
    }

  def buildRequest(request: SimpleRequest): IO[Request[IO]] =
    buildUri(request.baseUrl, request.endPoint).flatMap { uri =>
      val r = Request[IO](
        method = httpMethodToMethod(request.method),
        uri = uri
      ).withHeaders(
        request.headers.toHttp4sHeaders
      )

      request.body
        .map { b =>
          IO(r.withEntity(b))
        }
        .getOrElse(IO(r))
    }

  def buildResponse(status: IntAndReason, headers: Map[String, String], body: Option[String]): Response[IO] =
    intToStatus(status) match {
      case Left(l) =>
        l.toHttpResponse[IO](HttpVersion.`HTTP/1.1`)
      case Right(code) =>
        val response = Response[IO](
          status = code
        ).withHeaders(headers.toHttp4sHeaders)

        body
          .map { b =>
            response.withEntity(b)
          }
          .getOrElse(response)
    }

}

case class IntAndReason(code: Int, reason: Option[String])
