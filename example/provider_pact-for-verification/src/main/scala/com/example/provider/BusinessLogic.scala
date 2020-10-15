package com.example.provider

import java.io.File
import scala.io.Source
import scala.util.Random

object BusinessLogic {

  def loadPeople(peopleFile: String): List[String] = {
    val source = Source.fromFile(new File(peopleFile).toURI)
    val people = source
      .getLines
      .mkString
      .split(',')
      .toList

    source.close()

    people
  }

  def generateToken(requiredLength: Int): String =
    Random.alphanumeric.take(requiredLength).mkString

}
