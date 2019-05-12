package com.itv.scalapact.plugin.shared

import java.io.File

import com.itv.scalapact.shared.{Pact, PactLogger, ScalaPactSettings}
import com.itv.scalapact.shared.ColourOutput._

import scala.io.Source
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter}

import scala.util.Try

object ScalaPactTestCommand {

  def doPactPack(scalaPactSettings: ScalaPactSettings)(implicit pactReader: IPactReader,
                                                       pactWriter: IPactWriter): Unit = {
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
        squashPactFiles(scalaPactSettings.giveOutputPath, g._2)
      }.sum

      PactLogger.message(("> " + groupedFileList.length.toString + " pacts found:").white.bold)
      PactLogger.message(groupedFileList.map(g => "> - " + g._1.replace("_", " -> ") + "\n").mkString)
      PactLogger.message("> " + errorCount.toString + " errors")

    } else {
      PactLogger.error(
        s"No Pact files found in '${scalaPactSettings.giveOutputPath}'. Make sure you have Pact CDC tests and have run 'sbt test' or 'sbt pact-test'.".red
      )
    }
  }

  private def squashPactFiles(outputPath: String, files: List[File])(implicit pactReader: IPactReader,
                                                                     pactWriter: IPactWriter): Int = {
    val jsonStringToPact: String => Option[Pact] = pactReader.jsonStringToPact(_).toOption

    val (failed, squashedPacts) =
      files
        .map(fileToJsonString(_).flatMap(jsonStringToPact))
        .partition(_.isEmpty)

    squashedPacts
      .collect { case Some(pact) => pact }
      .reduceOption(combinePacts)
      .foreach { combined =>
        PactContractWriter.writePactContracts(outputPath)(combined.provider.name)(combined.consumer.name)(
          pactWriter.pactToJsonString(combined)
        )
      }

    failed.size
  }

  private def fileToJsonString(file: File): Option[String] =
    Try {
      val contents = Source.fromFile(file).getLines().mkString
      file.delete()
      contents
    }.toOption.orElse {
      PactLogger.message(("Problem reading Pact file at path: " + file.getCanonicalPath).red)
      None
    }

  private def combinePacts(p1: Pact, p2: Pact): Pact =
    p1.copy(interactions = p1.interactions ++ p2.interactions)
}
