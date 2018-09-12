package com.itv.scalapact.shared

case class PactVerifyResult(pact: Pact, results: List[PactVerifyResultInContext])
