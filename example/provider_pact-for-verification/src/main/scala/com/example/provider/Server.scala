package com.example.provider

import java.util.concurrent.Executors

import cats.effect.IO
import cats.effect._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server._

import scala.concurrent.ExecutionContext

object AlternateStartupApproach extends IOApp {
  override val executionContext = ExecutionContext.fromExecutor(
    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  )

  // On ordinary start up, this service will begin here.
  def run(args: List[String]): IO[ExitCode] =
    serverResource(BusinessLogic.loadPeople, BusinessLogic.generateToken)
      .use(_ => IO.never).as(ExitCode.Success)

  def serverResource(loadPeopleData: String => List[String], genToken: Int => String): Resource[IO, Server[IO]] =
    BlazeServerBuilder[IO](executionContext)
      .bindHttp(8080)
      .withHttpApp(Provider.service(loadPeopleData)(genToken))
      .resource

}
