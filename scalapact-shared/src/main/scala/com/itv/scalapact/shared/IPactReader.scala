package com.itv.scalapact.shared

trait IPactReader {

  type ReadPactF = String => Either[String, Pact]

  val readPact: ReadPactF = p => jsonStringToPact(p)

  def jsonStringToPact(json: String): Either[String, Pact]

}
