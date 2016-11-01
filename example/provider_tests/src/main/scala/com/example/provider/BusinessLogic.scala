package com.example.provider

import java.io.File
import scala.io.Source
import scala.util.Random

object BusinessLogic {

  def loadPeople(peopleFile: String): List[String] = {
    Source.fromFile(new File(peopleFile).toURI)
      .getLines
      .mkString
      .split(',')
      .toList
  }

  def generateToken(requiredLength: Int): String = {
    Random.alphanumeric.take(requiredLength).mkString
  }

}
