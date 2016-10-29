package com.itv.scalapact.plugin.tester

import java.io.File

import com.itv.scalapact.plugin._
import com.itv.scalapact.plugin.utils.{FileManipulator, FileSystemManipulator}
import com.itv.scalapactcore.{Pact, ScalaPactReader, ScalaPactWriter}


object PactFileWriter {
  def apply(): PactFileWriter = PactFileWriter(FileSystemManipulator, ScalaPactWriter.pactToJsonString, "target/pacts")

  val simplifyName: String => String = name => "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")
}

case class PactFileWriter(fileManipulator: FileManipulator, pactFormatter: PactFormatter, dirPath: String = "target/pacts") extends (Pact => Unit) {

  import PactFileWriter.{simplifyName}

  val dirFile = new File(dirPath)

  def path(pact: Pact) = dirPath + "/" + simplifyName(pact.consumer.name) + "_" + simplifyName(pact.provider.name) + ".json"

  def apply(pact: Pact) = {
    fileManipulator.createDirIfNeeded(dirFile)
    val relativePath = path(pact)
    val file = new File(relativePath)
    fileManipulator.deleteIfExists(file)
    fileManipulator.save(file, pactFormatter(pact))
    Unit
  }
}
