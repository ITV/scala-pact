package com.itv.scalapact

object ScalaPactVerify {

  object verifyPact extends VerifyPactElements {
    protected val strict: Boolean = false
  }

  object verifyStrictPact extends VerifyPactElements {
    protected val strict: Boolean = true
  }

  sealed trait VerifyPactElements {

    protected val strict: Boolean

    def between(consumer: String): ScalaPartialPactVerify = new ScalaPartialPactVerify(consumer)

    class ScalaPartialPactVerify(consumer: String) {
      def and(provider: String): ScalaPactVerifySource = new ScalaPactVerifySource(consumer, provider)
    }

    class ScalaPactVerifySource(consumer: String, provider: String) {
      def withPactSource(sourceType: PactSourceType): ScalaPactVerifyProviderStates = new ScalaPactVerifyProviderStates(consumer, provider, sourceType)
    }

    class ScalaPactVerifyProviderStates(consumer: String, provider: String, sourceType: PactSourceType) {
      def setupProviderState(providerStateMatch: PartialFunction[String, Boolean]): ScalaPactVerifyRunner = new ScalaPactVerifyRunner(consumer, provider, sourceType, providerStateMatch)
    }

    class ScalaPactVerifyRunner(consumer: String, provider: String, sourceType: PactSourceType, providerStateMatch: PartialFunction[String, Boolean]) {
      def runVerificationAgainst(serviceAddress: String): Unit = {

      }
    }

  }

  sealed trait PactSourceType
  case class directory(path: String) extends PactSourceType
  case class pactBroker(url: String) extends PactSourceType {
    def withContractVersion(version: String): pactBrokerWithVersion = pactBrokerWithVersion(url, version)
  }
  case class pactBrokerWithVersion(url: String, contractVersion: String) extends PactSourceType {
    def withContractVersion(version: String): pactBrokerWithVersion = pactBrokerWithVersion(url, version)
  }

}
