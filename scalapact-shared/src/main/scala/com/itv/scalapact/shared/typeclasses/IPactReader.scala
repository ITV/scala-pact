package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{IJsonConversionFunctions, Pact}

trait IPactReader extends IJsonConversionFunctions {

  type ReadPactF = String => Either[String, Pact]

  val readPact: ReadPactF = p => jsonStringToPact(p)

  def jsonStringToPact(json: String): Either[String, Pact]

}
