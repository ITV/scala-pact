package com.itv.scalapact.shared

final case class PactVerifyResult(pact: Pact, results: List[PactVerifyResultInContext])

final case class PactVerifyResultInContext(result: Either[String, Interaction], context: String)
