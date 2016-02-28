package com.itv.plugin

import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.CaseInsensitiveString
import sbt._

object ScalaPactStubberCommand {
  lazy val pactStubberCommandHyphen: Command = Command.args("pact-stubber", "<options>")(pactStubber)
  lazy val pactStubberCommandCamel: Command = Command.args("pactStubber", "<options>")(pactStubber)

  private lazy val pactStubber: (State, Seq[String]) => State = (state, args) => {
    (StubArguments.parseArguments andThen LocalFileLoader.loadPactFiles andThen startServer)(args)

    state
  }

  private lazy val startServer: Arguments => Unit = config => {
    println("Starting ScalaPact Stubber on: http://" + config.host + ":" + config.port)

    BlazeBuilder.bindHttp(config.port, config.host)
      .mountService(PactStubService.service, "/")
      .run
      .awaitShutdown()
  }

}

object StubArguments {
  lazy val parseArguments: Seq[String] => Arguments = args =>
    (pair andThen buildConfigMap)(args.toList)

  private lazy val pair: List[String] => Map[String, String] = list => {
    @annotation.tailrec
    def rec[A](l: List[A], acc: List[Map[A, A]]): List[Map[A, A]] = {
      l match {
        case Nil => acc
        case x :: Nil => acc
        case x :: xs => rec(l.drop(2), Map(x -> xs.head) :: acc)
      }
    }

    rec(list, Nil).foldLeft(Map[String, String]())(_ ++ _)
  }

  private lazy val buildConfigMap: Map[String, String] => Arguments = argMap =>
    Arguments(
      host = argMap.getOrElse("--host", "localhost"),
      port = argMap.get("--port").flatMap(safeStringToInt).getOrElse(1234),
      localPactPath = argMap.get("--source")
    )

  private lazy val safeStringToInt: String => Option[Int] = s =>
    try {
      Option(s.toInt)
    } catch {
      case e: Throwable => None
    }
}

object LocalFileLoader {

  private val recursiveJsonLoad: File => List[String] = path => {
    @annotation.tailrec
    def rec(files: List[File], acc: List[String]): List[String] = {
      files match {
        case Nil =>
          acc

        case x :: xs if !x.exists() =>
          println("Supplied pact path does not exist! Aborting file load.")
          Nil

        case x :: xs if x.isDirectory =>
          rec(x.listFiles().toList.filter(_.getName.endsWith(".json")) ++ files, acc)

        case x :: xs if x.isFile =>
          println("Loading pact file: " + x.getName)
          rec(files, scala.io.Source.fromURL(x.toURI.toURL).getLines().mkString("\n") :: acc)

        case _ =>
          println("Aborting, problem reading the pact files at location: " + path.getCanonicalPath)
          Nil
      }
    }

    try {
      rec(List(path).filter(_.getName.endsWith(".json")), Nil)
    } catch {
      case e: SecurityException =>
        println("Did not have permission to access the provided path, message:\n" + e.getMessage)
        Nil
      case e: Throwable =>
        println("Problem reading from supplied path, message:\n" + e.getMessage)
        Nil
    }
  }

  private val deserialiseIntoPact: List[String] => List[Pact] = pactJsonStrings => {
    pactJsonStrings.map { json =>
      //TODO: More nonsense
      Pact("fish")
    }
  }

  private val addToPactManager: List[Pact] => Unit = pacts => {
    // TODO: What Pact manager?!?

    ()
  }

  lazy val loadPactFiles: Arguments => Arguments = config => {
    // Side effecting, might as well be since the pacts are held statefully /
    // mutably so that they can be updated. The only way around this, I think,
    // would be to start new servers on update? Or some sort of foldp to update
    // the model?
    config.localPactPath match {
      case Some(path) =>
        (recursiveJsonLoad andThen deserialiseIntoPact andThen addToPactManager) (new File(path))

      case None => ()
    }

    config
  }

}

// TODO: Clearly nonesense...
case class Pact(id: String)

case class Arguments(host: String, port: Int, localPactPath: Option[String])

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