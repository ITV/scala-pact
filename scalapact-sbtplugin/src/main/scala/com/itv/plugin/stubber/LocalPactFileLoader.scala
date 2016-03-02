package com.itv.plugin.stubber

import java.io.File

import com.itv.scalapactcore.{Pact, ScalaPactReader}
import sbt._

import scalaz.\/-

object LocalPactFileLoader {

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
