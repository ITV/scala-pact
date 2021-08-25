package com.example.provider

import java.util.concurrent.Executors

import cats.effect.IO
import cats.effect._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server._

object AlternateStartupApproach extends IOApp {

  // On ordinary start up, this service will begin here.
  def run(args: List[String]): IO[ExitCode] =
    serverResource(BusinessLogic.loadPeople, BusinessLogic.generateToken)
      .use(_ => IO.never)
      .as(ExitCode.Success)

  def serverResource(loadPeopleData: String => List[String], genToken: Int => String): Resource[IO, Server] =
    Resource.eval(Sync[IO].executionContext).flatMap { ec =>
      BlazeServerBuilder[IO](ec)
        .bindHttp(8080)
        .withHttpApp(Provider.service(loadPeopleData)(genToken))
        .resource
    }

}
