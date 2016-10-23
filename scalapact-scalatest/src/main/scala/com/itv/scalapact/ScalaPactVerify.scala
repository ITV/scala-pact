package com.itv.scalapact

import com.itv.scalapactcore.common.Arguments
import com.itv.scalapactcore.common.Helpers
import com.itv.scalapactcore.verifier.{PactVerifySettings, ProviderState, Verifier, VersionedConsumer}

import java.io.{File, FileWriter, BufferedWriter}

import scala.language.implicitConversions

object ScalaPactVerify {
  implicit def toOption[A](a: A): Option[A] = Option(a)

  object verifyPact {

    def withPactSource(sourceType: PactSourceType): ScalaPactVerifyProviderStates = new ScalaPactVerifyProviderStates(sourceType)

    class ScalaPactVerifyProviderStates(sourceType: PactSourceType) {
      def setupProviderState(given: String)(setupProviderState: String => Boolean): ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, given, setupProviderState)
      def noSetupRequired: ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, None, None)
    }

    class ScalaPactVerifyRunner(sourceType: PactSourceType, given: Option[String], setupProviderState: Option[String => Boolean]) {

      def runStrictVerificationAgainst(port: Int): Unit = doVerification("http", "localhost", port, true)

      def runStrictVerificationAgainst(host: String, port: Int): Unit = doVerification("http", host, port, true)

      def runStrictVerificationAgainst(protocol: String, host: String, port: Int): Unit = doVerification(protocol, host, port, true)

      def runStrictVerificationAgainst(target: VerifyTargetConfig): Unit = doVerification(target.protocol, target.host, target.port, true)

      def runVerificationAgainst(port: Int): Unit = doVerification("http", "localhost", port, false)

      def runVerificationAgainst(host: String, port: Int): Unit = doVerification("http", host, port, false)

      def runVerificationAgainst(protocol: String, host: String, port: Int): Unit = doVerification(protocol, host, port, false)

      def runVerificationAgainst(target: VerifyTargetConfig): Unit = doVerification(target.protocol, target.host, target.port, false)

      private def doVerification(protocol: String, host: String, port: Int, strict: Boolean): Unit = {

        val providerStatesList =
          for {
            g  <- given
            ps <- setupProviderState
          } yield List(ProviderState(g, ps))

        val (verifySettings, arguments) = sourceType match {
          case pactAsJsonString(json) =>
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
  case class pactAsJsonString(json: String) extends PactSourceType

  class ScalaPactVerifyFailed extends Exception

  object VerifyTargetConfig {

    def apply(port: Int): VerifyTargetConfig = VerifyTargetConfig("http", "localhost", port)
    def apply(host: String, port: Int): VerifyTargetConfig = VerifyTargetConfig("http", host, port)

    def fromUrl(url: String): Option[VerifyTargetConfig] = {
      try {
        val pattern = """^([a-z]+):\/\/([a-z0-9\.\-_]+):(\d+).*""".r
        val pattern(protocol, host, port) = url.toLowerCase

        VerifyTargetConfig(protocol, host, Helpers.safeStringToInt(port).getOrElse(80))
      } catch {
        case e: Throwable =>
          println("Could not parse url '" + url + "', expected something like: http://localhost:80 (must specify the port!)")
          None
      }
    }

  }
  case class VerifyTargetConfig(protocol: String, host: String, port: Int)

}
