package com.itv.scalapact.http4s21.impl

import java.nio.charset.StandardCharsets

import cats.effect.IO
import com.itv.scalapact.shared.HttpMethod._
import com.itv.scalapact.shared.{HttpMethod, SimpleRequest}
import fs2.Chunk
import io.chrisdavenport.vault.Vault
import org.http4s._
import scodec.bits.ByteVector

object Http4sRequestResponseFactory {

  import HeaderImplicitConversions._
  import com.itv.scalapact.shared.RightBiasEither._

  implicit def toIO[A <: Throwable, B](a: Either[A, B]): IO[B] = a.fold(IO.raiseError, IO.pure)

  private val stringToByteVector: String => Chunk[Byte] = str => {
    Chunk.bytes(ByteVector(str.getBytes(StandardCharsets.UTF_8)).toArray)
  }

  def buildUri(baseUrl: String, endpoint: String): IO[Uri] =
    Uri.fromString(baseUrl + endpoint).leftMap(l => new Exception(l.message))

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
        uri = uri,
        httpVersion = HttpVersion.`HTTP/1.1`,
        headers = request.headers,
        body = EmptyBody,
        attributes = Vault.empty
      )

      request.body
        .map { b =>
          implicit val enc: EntityEncoder[IO, String] =
            EntityEncoder.simple[IO, String]()(stringToByteVector)

          IO(r.withEntity(b))
        }
        .getOrElse(IO(r))
    }

  def buildResponse(status: IntAndReason, headers: Map[String, String], body: Option[String]): IO[Response[IO]] =
    intToStatus(status) match {
      case Left(l) =>
        l.toHttpResponse(HttpVersion.`HTTP/1.1`)

      case Right(code) =>
        val response = Response[IO](
          status = code,
          httpVersion = HttpVersion.`HTTP/1.1`,
          headers = headers,
          body = EmptyBody,
          attributes = Vault.empty
        )

        body
          .map { b =>
            implicit val enc: EntityEncoder[IO, String] =
              EntityEncoder.simple[IO, String]()(stringToByteVector)
            IO(response.withEntity(b))
          }
          .getOrElse(IO(response))
    }

}

object HeaderImplicitConversions {
  implicit def mapToHeaderList(headerMap: Map[String, String]): Headers =
    Headers(headerMap.toList.map(t => Header(t._1, t._2)))

  implicit def headerListToMap(headers: Headers): Map[String, String] =
    headers.toList.map(h => Header.unapply(h)).collect { case Some(h) => (h._1.toString, h._2) }.toMap

  implicit def headerListToMaybeMap(headers: Headers): Option[Map[String, String]] =
    Option(headerListToMap(headers))
}

case class IntAndReason(code: Int, reason: Option[String])
