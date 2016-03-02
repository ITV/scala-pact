package com.itv.scalapact.plugin.tester

import java.io.PrintWriter

import com.itv.scalapactcore.{ScalaPactReader, ScalaPactWriter}
import sbt._

import scala.io.Source
import scala.language.implicitConversions

object ScalaPactTestCommand {

  lazy val pactTestCommandHyphen: Command = Command.command("pact-test")(pactTest)
  lazy val pactTestCommandCamel: Command = Command.command("pactTest")(pactTest)

  private lazy val pactTest: State => State = state => {

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

  private def squashPactFiles(name: String, files: List[File]): Int = {
    //Yuk!
    var errorCount = 0

    val pactList = {
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
          val option = ScalaPactReader.jsonStringToPact(t).toOption
          if (option.isEmpty) errorCount += 1
          option
        }
      }
        .collect { case Some(s) => s }
    }

    if (pactList.nonEmpty) {
      val head = pactList.head
      val combined = pactList.tail.foldLeft(head) { (accumulatedPact, nextPact) =>
        accumulatedPact.copy(interactions = accumulatedPact.interactions ++ nextPact.interactions)
      }

      ScalaPactContractWriter.writePactContracts(combined.provider.name)(combined.consumer.name)(ScalaPactWriter.pactToJsonString(combined))
    }

    errorCount
  }

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
