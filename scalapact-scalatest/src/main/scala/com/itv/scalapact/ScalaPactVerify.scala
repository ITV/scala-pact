package com.itv.scalapact

import com.itv.scalapactcore.common.Arguments
import com.itv.scalapactcore.verifier.{PactVerifySettings, ProviderState, Verifier, VersionedConsumer}

import java.io.{File, FileWriter, BufferedWriter}

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

    def withPactSource(sourceType: PactSourceType): ScalaPactVerifyProviderStates = new ScalaPactVerifyProviderStates(sourceType)

    class ScalaPactVerifyProviderStates(sourceType: PactSourceType) {
      def setupProviderState(given: String)(setupProviderState: String => Boolean): ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, given, setupProviderState)
      def noSetupRequired: ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, None, None)
    }

    class ScalaPactVerifyRunner(sourceType: PactSourceType, given: Option[String], setupProviderState: Option[String => Boolean]) {

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
          case pactContractString(json) =>
            val tmp = File.createTempFile("tmp_pact_", ".json")

            val fileWriter = new FileWriter(tmp, true)

            val bw = new BufferedWriter(fileWriter)
            bw.write(json)
            bw.close()

            (
              PactVerifySettings(
                providerStates = providerStatesList.getOrElse(Nil),
                pactBrokerAddress = "",
                projectVersion = "",
                providerName = "",
                consumerNames = Nil,
                versionedConsumerNames = Nil
              ),
              Arguments(
                host = host,
                protocol = protocol,
                port = port,
                localPactPath = tmp.getAbsolutePath(),
                strictMode = strict
              )
            )

          case loadFromLocal(path) =>
            (
              PactVerifySettings(
                providerStates = providerStatesList.getOrElse(Nil),
                pactBrokerAddress = "",
                projectVersion = "",
                providerName = "",
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

          case pactBroker(url, providerName, consumerNames) =>
            (
              PactVerifySettings(
                providerStates = providerStatesList.getOrElse(Nil),
                pactBrokerAddress = url,
                projectVersion = "",
                providerName = providerName,
                consumerNames = consumerNames,
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

          case pactBrokerWithVersion(url, version, providerName, consumerNames) =>
            (
              PactVerifySettings(
                providerStates = providerStatesList.getOrElse(Nil),
                pactBrokerAddress = url,
                projectVersion = "",
                providerName = providerName,
                consumerNames = Nil,
                versionedConsumerNames = consumerNames.map(c => VersionedConsumer(c, version))
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
  case class loadFromLocal(path: String) extends PactSourceType
  case class pactBroker(url: String, provider: String, consumers: List[String]) extends PactSourceType {
    def withContractVersion(version: String): pactBrokerWithVersion = pactBrokerWithVersion(url, version, provider, consumers)
  }
  case class pactBrokerWithVersion(url: String, contractVersion: String, provider: String, consumers: List[String]) extends PactSourceType {
    def withContractVersion(version: String): pactBrokerWithVersion = pactBrokerWithVersion(url, version, provider, consumers)
  }
  case class pactContractString(json: String) extends PactSourceType

  private class ScalaPactVerifyFailed extends Exception

}
