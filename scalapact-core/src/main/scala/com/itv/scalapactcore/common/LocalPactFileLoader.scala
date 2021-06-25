package com.itv.scalapactcore.common

import java.io.File
import com.itv.scalapact.shared.{Contract, ScalaPactSettings}
import com.itv.scalapact.shared.utils.ColourOutput._
import com.itv.scalapact.shared.json.ContractDeserializer
import com.itv.scalapact.shared.utils.PactLogger

object LocalPactFileLoader {

  private def recursiveDeserialize[P <: Contract: ContractDeserializer](allowTmpFiles: Boolean, file: File): List[P] = {
    @annotation.tailrec
    def rec(files: List[File], acc: List[P]): List[P] =
      files match {
        case Nil =>
          if (acc.isEmpty) {
            PactLogger.warn("WARNING: No pact files found.".yellow)
            acc
          } else {
            acc
          }

        case x :: _ if !x.exists() =>
          PactLogger.error("Supplied pact path does not exist! Aborting file load.".red)
          Nil

        case x :: xs if x.isDirectory =>
          PactLogger.debug("Found directory: " + x.getCanonicalPath)
          rec(x.listFiles().toList ++ xs, acc)

        case x :: xs if x.isFile && (x.getName.endsWith("_tmp.json") && allowTmpFiles) || x.getName.endsWith(".json") =>
          PactLogger.debug(("Loading pact file: " + x.getName).bold)
          val source = scala.io.Source.fromURL(x.toURI.toURL)
          val lines  = source.getLines().mkString("\n")
          source.close()
          rec(xs, deserializeIntoPact(x.getName, lines).toList ::: acc)

        case x :: xs if x.isFile && x.getName.endsWith("_tmp.json") && !allowTmpFiles =>
          PactLogger.error(("Ignoring temp pact file (did you run pactPack?): " + x.getName).yellow.bold)
          rec(xs, acc)

        case x :: xs =>
          PactLogger.warn(("Ignoring non-JSON file: " + x.getName).yellow)
          rec(xs, acc)

        case _ =>
          PactLogger.error(("Aborting, problem reading the pact files at location: " + file.getCanonicalPath).red)
          Nil
      }

    try rec(List(file), Nil)
    catch {
      case e: SecurityException =>
        PactLogger.error(("Did not have permission to access the provided path, message:\n" + e.getMessage).red)
        Nil
      case e: Throwable =>
        PactLogger.error(("Problem reading from supplied path, message:\n" + e.getMessage).red)
        Nil
    }
  }

  private def deserializeIntoPact[P <: Contract: ContractDeserializer](
      fileName: String,
      pactJsonString: String
  ): Option[P] = {
    val jsonOrError = ContractDeserializer[P].read(pactJsonString)
    jsonOrError.fold(
      error => {
        PactLogger.error(s"Problem deserializing pact file '$fileName':\n$error".red)
        None
      },
      c => Option(c)
    )
  }

  def loadPactFiles[P <: Contract: ContractDeserializer](allowTmpFiles: Boolean, defaultLocation: String)(
      config: ScalaPactSettings
  ): List[P] = {
    // Side effecting, might as well be since the pacts are held statefully /
    // mutably so that they can be updated. The only way around this, I think,
    // would be to start new servers on update? Or some sort of foldp to update
    // the model?
    PactLogger.debug(
      ("Looking for pact files in: " + config.localPactFilePath
        .orElse(Option(defaultLocation))
        .getOrElse("")).white.bold
    )

    config.localPactFilePath.orElse(Option(defaultLocation)) match {
      case Some(path) =>
        recursiveDeserialize[P](allowTmpFiles, new File(path))

      case None => Nil
    }
  }

}
