package com.itv.scalapact

//Should compile
class PactVerifySuiteSpec extends PactVerifySuite {
  val verify = verifyPact
    .withPactSource(loadFromLocal(""))
    .noSetupRequired
    .runVerificationAgainst(80)
}
