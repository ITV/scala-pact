package com.itv.scalapact.plugin.tester

import java.io.File

import com.itv.scalapact.plugin.utils.{FileManipulator, FileSystemManipulator}
import com.itv.scalapactcore.common.ColourOuput._
import com.itv.scalapactcore.{Pact, ScalaPactReader}
import sbt._

import scala.language.implicitConversions


case class PactAggregator(fileManipulator: FileManipulator = FileSystemManipulator, pactParser: String => Option[Pact] = t => ScalaPactReader.jsonStringToPact(t).toOption) extends (SquashDefn => Option[Pact]) {
  def apply(squashed: SquashDefn): Option[Pact] = {
    import squashed.files
    val (pacts, errorCount) = files.foldLeft((List[Pact]()), 0) { case ((pacts, error), file) => pactParser(fileManipulator.loadFileAndDelete(file)).fold((pacts, error + 1))(p => (pacts :+ p, error)) }
    println("> " + errorCount + " errors")
    pacts match {
      case head :: tail => Some(tail.foldLeft(head)((acc, p) => acc.copy(interactions = acc.interactions ++ p.interactions)))
      case _ => None
    }
  }
}

case class SquashDefn(name: String, files: List[File])


object GroupOfPactFiles {
  def apply(pactDir: File): Option[GroupOfPactFiles] = {
    if (pactDir.exists && pactDir.isDirectory) {
      Some(GroupOfPactFiles(pactDir.listFiles().toList.filter { f =>
        println(s"Found $f")
        f.getName.endsWith(".json") }))
    }
    else None
  }

  def applyWithMessages(pactDir: File) = {
    val result = apply(pactDir)
    result match {
      case Some(GroupOfPactFiles(files)) =>
        println(("> " + files.length + " files found that could be Pact contracts").white.bold)
        println(files.map("> - " + _.getName).mkString("\n").white)
      case None =>
        println("No Pact files found in 'target/pacts'. Make sure you have Pact CDC tests and have run 'sbt test' or 'sbt pact-test'.".red)
    }
    result
  }
}

case class GroupOfPactFiles(files: List[File]) {
  val groupedFileList: List[SquashDefn] = files.groupBy { f =>
    f.getName.split("_").take(2).mkString("_")
  }.toList.map { case (name, files) => SquashDefn(name, files) }


  def squash(squasher: PactSquasher) = {
    groupedFileList.foreach(squasher)

    println(("> " + groupedFileList.length + " pacts found:").white.bold)
    println(groupedFileList.map(g => "> - " + g.name.replace("_", " -> ") + "\n").mkString)
  }
}

case class PactSquasher(aggregator: PactAggregator, pactFileWriter: PactFileWriter) extends (SquashDefn => Unit) {
  def apply(squashDefn: SquashDefn) = aggregator(squashDefn).foreach(pactFileWriter)
}

object PactSquasher {
  def apply(): PactSquasher = PactSquasher(PactAggregator(), PactFileWriter())
}


object ScalaPactTestCommand {

  lazy val pactTestCommandHyphen: Command = Command.command("pact-test")(pactTest)
  lazy val pactTestCommandCamel: Command = Command.command("pactTest")(pactTest)

  lazy val squasher = PactSquasher()

  val pactDir = new java.io.File("target/pacts")

  private lazy val pactTest: State => State = state => {
    GroupOfPactFiles.applyWithMessages(pactDir) foreach (_.squash(squasher))
    state
  }

}