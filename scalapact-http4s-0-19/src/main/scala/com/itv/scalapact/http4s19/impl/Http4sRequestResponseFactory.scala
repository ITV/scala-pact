package com.itv.scalapact.http4s18.impl

import java.nio.charset.StandardCharsets

import cats.effect.IO
import com.itv.scalapact.shared.HttpMethod._
import com.itv.scalapact.shared.{HttpMethod, SimpleRequest}
import fs2.Chunk
import org.http4s._

object Http4sRequestResponseFactory {

  import HeaderImplicitConversions._
  import com.itv.scalapact.shared.RightBiasEither._

  implicit def toIO[A <: Throwable, B](a: Either[A, B]): IO[B] = a.fold(IO.raiseError, IO.pure)

  private val stringToByteVector: String => Chunk[Byte] = str => {
    Chunk.array(str.getBytes(StandardCharsets.UTF_8))
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
    }

  def buildRequest(request: SimpleRequest): IO[Request[IO]] =
    buildUri(request.baseUrl, request.endPoint).map { uri =>
      val r = Request[IO](
        method = httpMethodToMethod(request.method),
        uri = uri,
        httpVersion = HttpVersion.`HTTP/1.1`,
        headers = request.headers,
        body = EmptyBody,
        attributes = AttributeMap.empty
      )

      request.body
        .map { b =>
          implicit val enc: EntityEncoder[IO, String] =
            EntityEncoder.simple[IO, String]()(stringToByteVector)

          r.withEntity(b)
        }
        .getOrElse(r)
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
          attributes = AttributeMap.empty
        )

        IO.pure {
          body
            .map { b =>
              implicit val enc: EntityEncoder[IO, String] =
                EntityEncoder.simple[IO, String]()(stringToByteVector)

              response.withEntity(b)
            }
            .getOrElse(response)
        }
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
