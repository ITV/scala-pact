package com.itv.scalapact.shared.typeclasses

import com.itv.scalapact.shared.{HALIndex, IJsonConversionFunctions, Pact, PactsForVerification}

trait IPactReader extends IJsonConversionFunctions {
  def jsonStringToPact(json: String): Either[String, Pact]
  def jsonStringToPactsForVerification(json: String): Either[String, PactsForVerification]
  def jsonStringToHALIndex(json: String): Either[String, HALIndex]
}
