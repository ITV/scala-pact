package com.example.provider

import cats.effect._
import org.http4s.blaze.server.BlazeServerBuilder
import fs2.Stream

object Server extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Stream
      .eval(Sync[IO].executionContext)
      .flatMap { ec =>
        BlazeServerBuilder[IO](ec)
          .bindHttp(8080)
          .withHttpApp(Provider.service)
          .serve
      }
      .compile
      .drain
      .flatMap(_ => IO.never)
      .as(ExitCode.Success)
}
