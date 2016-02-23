package com.itv.plugin

import java.io.PrintWriter

import argonaut.Argonaut._
import argonaut.{CodecJson, PrettyParams}
import com.itv.plugin.PactImplicits._
import sbt.Keys._
import sbt._

import scala.io.Source
import scala.language.implicitConversions

object ScalaPactPlugin extends Plugin {

  override lazy val settings = Seq(commands += myCommand)

  lazy val myCommand =
    Command.command("pact") { (state: State) =>

      println("*************************************")
      println("** ScalaPact: Running tests        **")
      println("*************************************")

      println("> ScalaPact running: clean + test commands first")

      val cleanState = Command.process("clean", state)
      val testedState = Command.process("test", cleanState)

      println("*************************************")
      println("** ScalaPact: Squashing Pact Files **")
      println("*************************************")

      val pactDir = new java.io.File("target/pacts")

      if (pactDir.exists && pactDir.isDirectory) {
        val files = pactDir.listFiles().toList.filter(f => f.getName.endsWith(".json"))

        println("> " + files.length + " files found that could be Pact contracts")
        println(files.map("> - " + _.getName).mkString("\n"))

        val groupedFileList: List[(String, List[File])] = files.groupBy { f =>
          f.getName.split("_").take(2).mkString("_")
        }.toList

        val errorCount = groupedFileList.map { g =>
          squashPactFiles(g._1, g._2)
        }.sum

        println("> " + groupedFileList.length + " pacts found:")
        println(groupedFileList.map(g => "> - " + g._1.replace("_", " -> ") + "\n").mkString)
        println("> " + errorCount + " errors")

      } else {
        println("No Pact files found in 'target/pacts'. Make sure you have Pact CDC tests and have run 'sbt test'.")
      }

      testedState
    }

  def squashPactFiles(name: String, files: List[File]): Int = {
    //Yuk!
    var errorCount = 0

    val pact = {
      files.map { file =>
        val fileContents = try {
          val contents = Option(Source.fromFile(file).getLines().mkString)

          file.delete()

          contents
        } catch {
          case e: Throwable =>
            println("Problem reading Pact file at path: " + file.getCanonicalPath)
            errorCount = errorCount + 1
            None
        }
        fileContents.flatMap(_.decodeOption[Pact])
      }
        .collect { case Some(s) => s }
        .foldLeft(Pact(PactActor(""), PactActor(""), List.empty)) { (combinedPact, pact) =>
          combinedPact.copy(
            interactions = combinedPact.interactions ++ pact.interactions,
            provider = pact.provider,
            consumer = pact.consumer
          )
        }
    }

    ScalaPactContractWriter.writePactContracts(pact)

    errorCount
  }
}

object ScalaPactContractWriter {

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

  private def producePactJson(pact: Pact): String = pact.asJson.pretty(PrettyParams.spaces2.copy(dropNullKeys = true))

  implicit private val intToBoolean: Int => Boolean = v => v > 0
  implicit private val stringToBoolean: String => Boolean = v => v != ""
  implicit private val mapToBoolean: Map[String, String] => Boolean = v => v.nonEmpty

  implicit private def valueToOptional[A](value: A)(implicit p: A => Boolean): Option[A] = if (p(value)) Option(value) else None

}

object PactImplicits {
  implicit lazy val PactCodecJson: CodecJson[Pact] = casecodec3(Pact.apply, Pact.unapply)(
    "provider", "consumer", "interactions"
  )

  implicit lazy val PactActorCodecJson: CodecJson[PactActor] = casecodec1(PactActor.apply, PactActor.unapply)(
    "name"
  )

  implicit lazy val InteractionCodecJson: CodecJson[Interaction] = casecodec4(Interaction.apply, Interaction.unapply)(
    "providerState", "description", "request", "response"
  )

  implicit lazy val InteractionRequestCodecJson: CodecJson[InteractionRequest] = casecodec4(InteractionRequest.apply, InteractionRequest.unapply)(
    "method", "path", "headers", "body"
  )

  implicit lazy val InteractionResponseCodecJson: CodecJson[InteractionResponse] = casecodec3(InteractionResponse.apply, InteractionResponse.unapply)(
    "status", "headers", "body"
  )
}

case class Pact(provider: PactActor, consumer: PactActor, interactions: List[Interaction])

case class PactActor(name: String)

case class Interaction(providerState: Option[String], description: String, request: InteractionRequest, response: InteractionResponse)

case class InteractionRequest(method: Option[String], path: Option[String], headers: Option[Map[String, String]], body: Option[String])

case class InteractionResponse(status: Option[Int], headers: Option[Map[String, String]], body: Option[String])
