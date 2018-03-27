package com.itv.scalapact.plugin.tester

import com.itv.scalapact.shared.ScalaPactSettings
import com.itv.scalapact.shared.ColourOuput._
import sbt._

import scala.io.Source
import scala.language.implicitConversions
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapact.shared.PactLogger
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter}

object ScalaPactTestCommand {

  def doPactPack(scalaPactSettings: ScalaPactSettings): Unit = {
    PactLogger.message("*************************************".white.bold)
    PactLogger.message("** ScalaPact: Squashing Pact Files **".white.bold)
    PactLogger.message("*************************************".white.bold)

    val pactDir = new java.io.File(scalaPactSettings.giveOutputPath)

    if (pactDir.exists && pactDir.isDirectory) {
      val files = pactDir.listFiles().toList.filter(f => f.getName.endsWith(".json"))

      PactLogger.message(("> " + files.length.toString + " files found that could be Pact contracts").white.bold)
      PactLogger.message(files.map("> - " + _.getName).mkString("\n").white)

      val groupedFileList: List[(String, List[File])] = files.groupBy { f =>
        f.getName.split("_").take(2).mkString("_")
      }.toList

      val errorCount = groupedFileList.map { g =>
        squashPactFiles(scalaPactSettings.giveOutputPath, g._1, g._2)
      }.sum

      PactLogger.message(("> " + groupedFileList.length.toString + " pacts found:").white.bold)
      PactLogger.message(groupedFileList.map(g => "> - " + g._1.replace("_", " -> ") + "\n").mkString)
      PactLogger.message("> " + errorCount.toString + " errors")

    } else {
      PactLogger.error(s"No Pact files found in '${scalaPactSettings.giveOutputPath}'. Make sure you have Pact CDC tests and have run 'sbt test' or 'sbt pact-test'.".red)
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
            PactLogger.message(("Problem reading Pact file at path: " + file.getCanonicalPath).red)
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
