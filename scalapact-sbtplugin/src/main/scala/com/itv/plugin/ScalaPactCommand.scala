package com.itv.plugin

import argonaut.Argonaut._
import argonaut.{Argonaut, Json}
import java.io.PrintWriter
import sbt._
import scala.io.Source
import scala.language.implicitConversions
import scalaz.PLensFamily

object ScalaPactCommand {

  lazy val pactCommand =
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
        fileContents.flatMap { t =>
          val option = t.parseOption
          if (option.isEmpty) errorCount += 1
          option
        }
      }
        .collect { case Some(s) => s }
    }

    if (pact.nonEmpty) {
      val head = pact.head
      val combined = pact.tail.foldLeft(head) { (acc, json) =>
        interactionsExtractor.mod(arrayCombiner(interactionsExtractor.get(json)), acc)
      }

      ScalaPactContractWriter.writePactContracts(providerExtractor.get(head).getOrElse(""))(consumerExtractor.get(head).getOrElse(""))(combined.spaces2)
    }

    errorCount
  }

  type NameExtractorReturnType = PLensFamily[Json, Json, Argonaut.JsonString, Argonaut.JsonString]
  val interactionsExtractor = jObjectPL >=> jsonObjectPL("interactions") >=> jArrayPL

  val arrayCombiner: Option[Argonaut.JsonArray] => Argonaut.JsonArray => Argonaut.JsonArray =
    option => array => option.map(a => array ++ a).getOrElse(array) // TODO increment errorCount when None?

  val nameExtractor: String => NameExtractorReturnType =
    field => jObjectPL >=> jsonObjectPL(field) >=> jObjectPL >=> jsonObjectPL("name") >=> jStringPL

  val providerExtractor = nameExtractor("provider")
  val consumerExtractor = nameExtractor("consumer")
}

object ScalaPactContractWriter {

  private val simplifyName: String => String = name =>
    "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")

  val writePactContracts: String => String => String => Unit = provider => consumer => contents => {
    val dirPath = "target/pacts"
    val dirFile = new File(dirPath)

    if (!dirFile.exists()) {
      dirFile.mkdir()
    }

    val relativePath = dirPath + "/" + simplifyName(consumer) + "_" + simplifyName(provider) + ".json"
    val file = new File(relativePath)

    if (file.exists()) {
      file.delete()
    }

    file.createNewFile()

    new PrintWriter(relativePath) {
      write(contents)
      close()
    }

    ()
  }
}
