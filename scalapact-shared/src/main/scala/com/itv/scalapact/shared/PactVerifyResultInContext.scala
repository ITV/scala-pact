package com.itv.scalapact.shared

case class PactVerifyResultInContext(result: Either[String, Interaction], context: String)
