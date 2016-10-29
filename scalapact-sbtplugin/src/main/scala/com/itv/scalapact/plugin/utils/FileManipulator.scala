package com.itv.scalapact.plugin.utils

import java.io.{File, PrintWriter}

import scala.io.Source

trait FileManipulator {

  def loadFile(file: File): String

  def createDirIfNeeded(dir: File)

  def deleteIfExists(file: File)

  def save(file: File, contents: String)

  def loadFileAndDelete(file: File): String = {
    val result = loadFile(file)
    deleteIfExists(file)
    result
  }
}

object FileSystemManipulator extends FileManipulator {
  def deleteIfExists(file: File): Unit = if (file.exists()) file.delete()

   def loadFile(file: File): String = Source.fromFile(file).getLines().mkString

  def save(file: File, contents: String): Unit = {
    file.createNewFile()

    new PrintWriter(file) {
      write(contents)
      close()
    }
  }

  def createDirIfNeeded(dir: File): Unit = if (!dir.exists()) dir.mkdir()

}

object FileManipulator {
  def apply() = FileSystemManipulator
}
