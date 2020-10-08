package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{IJsonConversionFunctions, Pact, PactsForVerification}

trait IPactReader extends IJsonConversionFunctions {
  def jsonStringToPact(json: String): Either[String, Pact]

  def jsonStringToPactsForVerification(json: String): Either[String, PactsForVerification]
}
