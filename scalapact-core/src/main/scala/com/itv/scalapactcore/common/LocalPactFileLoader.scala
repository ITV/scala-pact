package com.itv.scalapactcore.common

import java.io.File

import com.itv.scalapactcore.{Pact, ScalaPactReader}

import com.itv.scalapactcore.common.ColourOuput._

object LocalPactFileLoader {

  private val recursiveJsonLoad: File => List[String] = file => {
    @annotation.tailrec
    def rec(files: List[File], acc: List[String]): List[String] = {
      files match {
        case Nil =>
          if(acc.isEmpty) {
            println("WARNING: No pact files found.".yellow)
            acc
          } else {
            acc
          }

        case x :: xs if !x.exists() =>
          println("Supplied pact path does not exist! Aborting file load.".red)
          Nil

        case x :: xs if x.isDirectory =>
          println("Found directory: " + x.getCanonicalPath)
          rec(x.listFiles().toList ++ xs, acc)

        case x :: xs if x.isFile && x.getName.endsWith(".json") =>
          println(("Loading pact file: " + x.getName).bold)
          rec(xs, scala.io.Source.fromURL(x.toURI.toURL).getLines().mkString("\n") :: acc)

        case x :: xs =>
          println(("Ignoring non-JSON file: " + x.getName).yellow)
          rec(xs, acc)

        case _ =>
          println(("Aborting, problem reading the pact files at location: " + file.getCanonicalPath).red)
          Nil
      }
    }

    try {
      rec(List(file), Nil)
    } catch {
      case e: SecurityException =>
        println(("Did not have permission to access the provided path, message:\n" + e.getMessage).red)
        Nil
      case e: Throwable =>
        println(("Problem reading from supplied path, message:\n" + e.getMessage).red)
        Nil
    }
  }

  private val deserializeIntoPact: List[String] => List[Pact] = pactJsonStrings => {
    pactJsonStrings.map { json =>
      ScalaPactReader.jsonStringToPact(json)
    }.collect { case Right(p) => p }
  }

  lazy val loadPactFiles: String => Arguments => ConfigAndPacts = defaultLocation => config => {
    // Side effecting, might as well be since the pacts are held statefully /
    // mutably so that they can be updated. The only way around this, I think,
    // would be to start new servers on update? Or some sort of foldp to update
    // the model?

    println(("Looking for pact files in: " + config.localPactPath.orElse(Option(defaultLocation)).getOrElse("")).white.bold)

    val pacts = config.localPactPath.orElse(Option(defaultLocation)) match {
      case Some(path) =>
        (recursiveJsonLoad andThen deserializeIntoPact) (new File(path))

      case None => Nil
    }

    ConfigAndPacts(config, pacts)
  }

}

case class ConfigAndPacts(arguments: Arguments, pacts: List[Pact])
