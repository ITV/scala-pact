package com.itv.scalapact

import com.itv.scalapactcore.common.Arguments
import com.itv.scalapactcore.verifier.{PactVerifySettings, ProviderState, Verifier, VersionedConsumer}

import scala.language.implicitConversions

object ScalaPactVerify {
  implicit def toOption[A](a: A): Option[A] = Option(a)

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
      def setupProviderState(given: String)(setupProviderState: String => Boolean): ScalaPactVerifyRunner = new ScalaPactVerifyRunner(consumer, provider, sourceType, given, setupProviderState)
      def noSetupRequired: ScalaPactVerifyRunner = new ScalaPactVerifyRunner(consumer, provider, sourceType, None, None)
    }

    class ScalaPactVerifyRunner(consumer: String, provider: String, sourceType: PactSourceType, given: Option[String], setupProviderState: Option[String => Boolean]) {

      def runVerificationAgainst(port: Int): Unit = doVerification("http", "localhost", port)

      def runVerificationAgainst(host: String, port: Int): Unit = doVerification("http", host, port)

      def runVerificationAgainst(protocol: String, host: String, port: Int): Unit = doVerification(protocol, host, port)

      private def doVerification(protocol: String, host: String, port: Int): Unit = {

        val providerStatesList =
          for {
            g  <- given
            ps <- setupProviderState
          } yield List(ProviderState(g, ps))

        val (verifySettings, arguments) = sourceType match {
          case directory(path) =>
            (
              PactVerifySettings(
                providerStates = providerStatesList.getOrElse(Nil),
                pactBrokerAddress = "",
                projectVersion = "",
                providerName = provider,
                consumerNames = Nil,
                versionedConsumerNames = Nil
              ),
              Arguments(
                host = host,
                protocol = protocol,
                port = port,
                localPactPath = path,
                strictMode = strict
              )
            )

          case pactBroker(url) =>
            (
              PactVerifySettings(
                providerStates = providerStatesList.getOrElse(Nil),
                pactBrokerAddress = url,
                projectVersion = "",
                providerName = provider,
                consumerNames = List(consumer),
                versionedConsumerNames = Nil
              ),
              Arguments(
                host = host,
                protocol = protocol,
                port = port,
                localPactPath = None,
                strictMode = strict
              )
            )

          case pactBrokerWithVersion(url, version) =>
            (
              PactVerifySettings(
                providerStates = providerStatesList.getOrElse(Nil),
                pactBrokerAddress = url,
                projectVersion = "",
                providerName = provider,
                consumerNames = Nil,
                versionedConsumerNames = List(VersionedConsumer(consumer, version))
              ),
              Arguments(
                host = host,
                protocol = protocol,
                port = port,
                localPactPath = None,
                strictMode = strict
              )
            )
        }

        if(Verifier.verify(verifySettings)(arguments)) Unit else throw new ScalaPactVerifyFailed
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

  private class ScalaPactVerifyFailed extends Exception

}
