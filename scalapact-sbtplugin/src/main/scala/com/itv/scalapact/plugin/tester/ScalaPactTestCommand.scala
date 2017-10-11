package com.itv.scalapact.plugin.tester

import com.itv.scalapact.plugin.ScalaPactPlugin
import com.itv.scalapact.shared.{IPactReader, IPactWriter, ScalaPactSettings}
import com.itv.scalapact.shared.ColourOuput._
import sbt._

import scala.io.Source
import scala.language.implicitConversions
import com.itv.scalapactcore.common.PactReaderWriter._

object ScalaPactTestCommand {

  lazy val pactTestCommandHyphen: Command = Command.args("pact-test", "<options>")(pactTest)
  lazy val pactTestCommandCamel: Command = Command.args("pactTest", "<options>")(pactTest)

  private lazy val pactTest: (State, Seq[String]) => State = (state, args) => {

      println("*************************************".white.bold)
      println("** ScalaPact: Running tests        **".white.bold)
      println("*************************************".white.bold)

      println("> ScalaPact running: clean + test commands first")

      val cleanState = Command.process("clean", state)
      val testedState = Command.process("test", cleanState)

      doPactPack(Project.extract(testedState).get(ScalaPactPlugin.autoImport.scalaPactEnv).toSettings + ScalaPactSettings.parseArguments(args))

      testedState
    }

  def doPactPack(scalaPactSettings: ScalaPactSettings): Unit = {
    println("*************************************".white.bold)
    println("** ScalaPact: Squashing Pact Files **".white.bold)
    println("*************************************".white.bold)

    val pactDir = new java.io.File(scalaPactSettings.giveOutputPath)

    if (pactDir.exists && pactDir.isDirectory) {
      val files = pactDir.listFiles().toList.filter(f => f.getName.endsWith(".json"))

      println(("> " + files.length.toString + " files found that could be Pact contracts").white.bold)
      println(files.map("> - " + _.getName).mkString("\n").white)

      val groupedFileList: List[(String, List[File])] = files.groupBy { f =>
        f.getName.split("_").take(2).mkString("_")
      }.toList

      val errorCount = groupedFileList.map { g =>
        squashPactFiles(scalaPactSettings.giveOutputPath, g._1, g._2)
      }.sum

      println(("> " + groupedFileList.length.toString + " pacts found:").white.bold)
      println(groupedFileList.map(g => "> - " + g._1.replace("_", " -> ") + "\n").mkString)
      println("> " + errorCount.toString + " errors")

    } else {
      println(s"No Pact files found in '${scalaPactSettings.giveOutputPath}'. Make sure you have Pact CDC tests and have run 'sbt test' or 'sbt pact-test'.".red)
    }
  }

  private def squashPactFiles(outputPath: String, name: String, files: List[File])(implicit pactReader: IPactReader, pactWriter: IPactWriter): Int = {
    //Yuk!
    var errorCount = 0

    val pactList = {
      files.map { file =>
        val fileContents = try {
          val contents = Option(Source.fromFile(file).getLines().mkString)

          file.delete()

          contents
        } catch {
          case _: Throwable =>
            println(("Problem reading Pact file at path: " + file.getCanonicalPath).red)
            errorCount = errorCount + 1
            None
        }
        fileContents.flatMap { t =>
          pactReader.jsonStringToPact(t) match {
            case Right(r) => Option(r)
            case Left(_) =>
              errorCount += 1
              None
          }
        }
      }
        .collect { case Some(s) => s }
    }

    pactList match {
      case Nil =>
        ()

      case x :: xs =>
        val combined = xs.foldLeft(x) { (accumulatedPact, nextPact) =>
          accumulatedPact.copy(interactions = accumulatedPact.interactions ++ nextPact.interactions)
        }

        PactContractWriter.writePactContracts(outputPath)(combined.provider.name)(combined.consumer.name)(pactWriter.pactToJsonString(combined))

        ()
    }

    errorCount
  }

}
