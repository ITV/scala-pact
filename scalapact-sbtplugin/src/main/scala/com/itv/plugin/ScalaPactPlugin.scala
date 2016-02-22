package com.itv.plugin

import java.io.PrintWriter
import org.json4s.DefaultFormats
import org.json4s.native.JsonParser._
import org.json4s.native.Serialization._
import sbt.Keys._
import sbt._
import scala.io.Source

object ScalaPactPlugin extends Plugin {

  override lazy val settings = Seq(commands += myCommand)

  private implicit val formats = DefaultFormats

  lazy val myCommand =
    Command.command("hello") { (state: State) =>
      println("Hi!")
      val files = new sbt.File("target/pacts").listFiles()
      println("Files: " + files.length)
      // load files as json strings
      // parse json into Pact case classes
      // merge Interactions from Pacts
      val pact = files.map { file =>
      val source = Source.fromFile(file)
        try {
          val string = source.mkString
          println("String: \n" + string)
          parse(string).extract[Pact]
        } finally source.close
      }.foldLeft(Pact(PactActor(""), PactActor(""), List.empty))((ret, pact) => ret.copy(interactions = ret.interactions ++ pact.interactions, provider = pact.provider, consumer = pact.consumer))

      ScalaPactContractWriter.writePactContracts(pact)

      // write single json file
      // delete input files
      state
    }
}

object ScalaPactContractWriter {

  private implicit val formats = DefaultFormats

  private val simplifyName: String => String = name =>
    "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")

  val writePactContracts: Pact => Unit = pactDescription => {
    val dirPath = "target/pacts"
    val dirFile = new File(dirPath)

    if (!dirFile.exists()) {
      dirFile.mkdir()
    }

    val relativePath = dirPath + "/" + simplifyName(pactDescription.consumer.name) + "_" + simplifyName(pactDescription.provider.name) + ".json"
    val file = new File(relativePath)

    if (file.exists()) {
      file.delete()
    }

    file.createNewFile()

    new PrintWriter(relativePath) {
      write(producePactJson(pactDescription))
      close()
    }

    ()
  }

  private def producePactJson(pact: Pact): String = writePretty(pact)

  implicit private val intToBoolean: Int => Boolean = v => v > 0
  implicit private val stringToBoolean: String => Boolean = v => v != ""
  implicit private val mapToBoolean: Map[String, String] => Boolean = v => v.nonEmpty

  implicit private def valueToOptional[A](value: A)(implicit p: A => Boolean): Option[A] = if (p(value)) Option(value) else None

}

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])

case class PactActor(name: String)

case class Interaction(providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)

case class InteractionRequest(method: Option[String], path: Option[String], headers: Option[Map[String, String]], body: Option[AnyRef])

case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[AnyRef])
