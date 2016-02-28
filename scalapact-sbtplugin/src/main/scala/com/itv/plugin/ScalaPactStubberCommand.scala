package com.itv.plugin

import argonaut.Argonaut._
import org.http4s._
import org.http4s.server._
import org.http4s.dsl._
import org.http4s.util.CaseInsensitiveString
import org.http4s.server.blaze.BlazeBuilder
import sbt._

object ScalaPactStubberCommand {
  lazy val pactStubberCommandHyphen: Command = Command.args("pact-stubber", "<options>")(pactStubber)
  lazy val pactStubberCommandCamel: Command = Command.args("pactStubber", "<options>")(pactStubber)

  private lazy val pactStubber: (State, Seq[String]) => State = (state, args) => {

    val config = parseArguments(args)

    println("Starting ScalaPact Stubber on: http://" + config.host + ":" + config.port)

    serverStart(config)

    state
  }

  private def serverStart(config: Arguments): Unit = {
    BlazeBuilder.bindHttp(config.port, config.host)
      .mountService(PactStubService.service, "/")
      .run
      .awaitShutdown()
  }

  private def safeStringToInt(s: String): Option[Int] = {
    try {
      Option(s.toInt)
    } catch {
      case e: Throwable => None
    }
  }

  private def parseArguments(args: Seq[String]): Arguments = {
    val argMap = pair(args.toList)

    Arguments(
      host = argMap.getOrElse("--host", "localhost"),
      port = argMap.get("--port").flatMap(safeStringToInt).getOrElse(1234)
    )
  }

  def pair[A](list: List[A]): Map[A, A] = {
    @annotation.tailrec
    def rec(l: List[A], acc: List[Map[A, A]]): List[Map[A, A]] = {
      l match {
        case Nil => acc
        case x :: Nil => acc
        case x :: xs => rec(l.drop(2), Map(x -> xs.head) :: acc)
      }
    }

    rec(list, Nil).foldLeft(Map[A, A]())(_ ++ _)
  }
}

case class Arguments(host: String, port: Int)

object PactStubService {

  val isAdminCall: Request => Boolean = request =>
    request.pathInfo.startsWith("/interactions") &&
      request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  val service = HttpService {

    case req @ GET -> Root / path =>

      Ok("GET " + req.pathInfo + " isAdmin: " + isAdminCall(req))

    case req @ PUT -> Root / path =>
      Ok("PUT " + req.pathInfo + " isAdmin: " + isAdminCall(req))

    case req @ POST -> Root / path =>
      Ok("POST " + req.pathInfo + " isAdmin: " + isAdminCall(req))

    case req @ DELETE -> Root / path =>
      Ok("DELETE " + req.pathInfo + " isAdmin: " + isAdminCall(req))

  }
}