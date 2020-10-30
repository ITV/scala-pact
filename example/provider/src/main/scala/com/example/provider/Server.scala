package com.example.provider

import java.util.concurrent.Executors

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Server extends IOApp {
  val executionContext = ExecutionContext.fromExecutor(
    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  )

  def run(args: List[String]): IO[ExitCode] = BlazeServerBuilder[IO](executionContext)
    .bindHttp(8080)
    .withHttpApp(Provider.service)
    .serve.compile.drain
    .flatMap(_ => IO.never)
    .as(ExitCode.Success)
}
