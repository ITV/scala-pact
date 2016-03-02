package com.itv.plugin

import com.itv.scalapactcore.{Interaction, Pact, ScalaPactReader}
import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.CaseInsensitiveString
import sbt._

import scalaz.{-\/, \/-}

object ScalaPactStubberCommand {
  lazy val pactStubberCommandHyphen: Command = Command.args("pact-stubber", "<options>")(pactStubber)
  lazy val pactStubberCommandCamel: Command = Command.args("pactStubber", "<options>")(pactStubber)

  private lazy val pactStubber: (State, Seq[String]) => State = (state, args) => {
    val pactTestedState = Command.process("pact-test", state)

    (StubArguments.parseArguments andThen LocalFileLoader.loadPactFiles andThen startServer)(args)

    pactTestedState
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

  private val recursiveJsonLoad: File => List[String] = file => {
    @annotation.tailrec
    def rec(files: List[File], acc: List[String]): List[String] = {
      files match {
        case Nil =>
          acc

        case x :: xs if !x.exists() =>
          println("Supplied pact path does not exist! Aborting file load.")
          Nil

        case x :: xs if x.isDirectory =>
          rec(x.listFiles().toList ++ xs, acc)

        case x :: xs if x.isFile && x.getName.endsWith(".json") =>
          println("Loading pact file: " + x.getName)
          rec(xs, scala.io.Source.fromURL(x.toURI.toURL).getLines().mkString("\n") :: acc)

        case _ =>
          println("Aborting, problem reading the pact files at location: " + file.getCanonicalPath)
          Nil
      }
    }

    try {
      rec(List(file), Nil)
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
      ScalaPactReader.jsonStringToPact(json)
    }.collect { case \/-(p) => p }
  }

  private val addToInteractionManager: List[Pact] => Unit = pacts => {
    pacts.foreach { p =>
      println(">Adding interactions:\n> - " + p.interactions.mkString("\n> - "))
      InteractionManager.addInteractions(p.interactions)
    }
  }

  lazy val loadPactFiles: Arguments => Arguments = config => {
    // Side effecting, might as well be since the pacts are held statefully /
    // mutably so that they can be updated. The only way around this, I think,
    // would be to start new servers on update? Or some sort of foldp to update
    // the model?
    config.localPactPath.orElse(Option("target/pacts")) match {
      case Some(path) =>
        (recursiveJsonLoad andThen deserialiseIntoPact andThen addToInteractionManager) (new File(path))

      case None => ()
    }

    config
  }

}

object InteractionManager {

  import HeaderImplicitConversions._

  private var interactions = List.empty[Interaction]

  def findMatchingInteraction(request: Request): Option[Interaction] = {

    val method = Option(request.method.name.toUpperCase)
    val headers: Option[Map[String, String]] = Option(request.headers)
    val path = Option(request.pathInfo) //TODO: Not good enough!
    val body = request.bodyAsText.runLast.run

    println("Trying to match: " + method + ", " + path + ", " + headers + ", " + body + ", ")

    interactions.find{ i =>
      i.request.method == method &&
        i.request.headers.toSet.subsetOf(headers.toSet) &&
        i.request.path == path &&
        i.request.body == body
    }
  }

  def getInteractions: List[Interaction] = interactions

  def addInteraction(interaction: Interaction): Unit = interactions = interaction :: interactions

  def addInteractions(interactions: List[Interaction]): Unit = interactions.foreach(addInteraction)

  def clearInteractions(): Unit = interactions = List.empty[Interaction]

}

case class Arguments(host: String, port: Int, localPactPath: Option[String])

object PactStubService {

  val isAdminCall: Request => Boolean = request =>
    request.pathInfo.startsWith("/interactions") &&
      request.headers.get(CaseInsensitiveString("X-Pact-Admin")).exists(h => h.value == "true")

  val service = HttpService {

    case req @ GET -> Root / path =>
      matchRequestWithResponse(req)

    case req @ PUT -> Root / path =>
      matchRequestWithResponse(req)

    case req @ POST -> Root / path =>
      matchRequestWithResponse(req)

    case req @ DELETE -> Root / path =>
      matchRequestWithResponse(req)

  }

  def matchRequestWithResponse(req: Request): scalaz.concurrent.Task[Response] = {
    if(isAdminCall(req)) Ok(InteractionManager.getInteractions.mkString("\n"))
    else {
      val interaction = InteractionManager.findMatchingInteraction(req)

      if(interaction.isEmpty) NotFound("No interaction found for request: " + req.method.name.toUpperCase + " " + req.pathInfo)
      else {

        val i = interaction.get

        Status.fromInt(i.response.status.getOrElse(200)) match {
          case \/-(code) =>
            Http4sRequestResponseFactory.buildResponse(
              status = code,
              headers = i.response.headers.getOrElse(Map.empty),
              body = i.response.body
            )

          case -\/(l) => InternalServerError(l.sanitized)
        }
      }
    }
  }
}