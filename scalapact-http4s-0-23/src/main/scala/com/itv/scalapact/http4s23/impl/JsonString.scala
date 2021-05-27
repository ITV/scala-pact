package com.itv.scalapact.http4s23.impl

import cats.effect.IO
import org.http4s.{EntityEncoder, MediaType}
import org.http4s.headers.`Content-Type`

final case class JsonString(value: String)

object JsonString {
  implicit val entityEncoder: EntityEncoder[IO, JsonString] =
    EntityEncoder
      .stringEncoder[IO]
      .contramap[JsonString](_.value)
      .withContentType(`Content-Type`(MediaType.application.json))
}
