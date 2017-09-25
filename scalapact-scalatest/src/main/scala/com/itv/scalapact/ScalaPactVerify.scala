package com.itv.scalapact

import com.itv.scalapactcore.common.{Helpers, LocalPactFileLoader}
import com.itv.scalapactcore.verifier.{PactVerifySettings, Verifier, VersionedConsumer}
import java.io.{BufferedWriter, File, FileWriter}

import com.itv.scalapact.shared.Arguments
import com.itv.scalapactcore.common.PactReaderWriter._

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

      def runStrictVerificationAgainst(port: Int): Unit = doVerification("http", "localhost", port, VerifyTargetConfig.defaultClientTimeoutInSeconds, strict = true)
      def runStrictVerificationAgainst(port: Int, clientTimeoutInSeconds: Int): Unit = doVerification("http", "localhost", port, clientTimeoutInSeconds, strict = true)
      def runStrictVerificationAgainst(host: String, port: Int): Unit = doVerification("http", host, port, VerifyTargetConfig.defaultClientTimeoutInSeconds, strict = true)
      def runStrictVerificationAgainst(host: String, port: Int, clientTimeoutInSeconds: Int): Unit = doVerification("http", host, port, clientTimeoutInSeconds, strict = true)
      def runStrictVerificationAgainst(protocol: String, host: String, port: Int): Unit = doVerification(protocol, host, port, VerifyTargetConfig.defaultClientTimeoutInSeconds, strict = true)
      def runStrictVerificationAgainst(target: VerifyTargetConfig): Unit = doVerification(target.protocol, target.host, target.port, target.clientTimeoutInSeconds, strict = true)

      def runVerificationAgainst(port: Int): Unit = doVerification("http", "localhost", port, VerifyTargetConfig.defaultClientTimeoutInSeconds, strict = false)
      def runVerificationAgainst(port: Int, clientTimeoutInSeconds: Int): Unit = doVerification("http", "localhost", port, clientTimeoutInSeconds, strict = false)
      def runVerificationAgainst(host: String, port: Int): Unit = doVerification("http", host, port, VerifyTargetConfig.defaultClientTimeoutInSeconds, strict = false)
      def runVerificationAgainst(host: String, port: Int, clientTimeoutInSeconds: Int): Unit = doVerification("http", host, port, clientTimeoutInSeconds, strict = false)
      def runVerificationAgainst(protocol: String, host: String, port: Int): Unit = doVerification(protocol, host, port, VerifyTargetConfig.defaultClientTimeoutInSeconds, strict = false)
      def runVerificationAgainst(protocol: String, host: String, port: Int, clientTimeoutInSeconds: Int): Unit = doVerification(protocol, host, port, clientTimeoutInSeconds, strict = false)
      def runVerificationAgainst(target: VerifyTargetConfig): Unit = doVerification(target.protocol, target.host, target.port, target.clientTimeoutInSeconds, strict = false)

      private def doVerification(protocol: String, host: String, port: Int, clientTimeoutInSeconds: Int, strict: Boolean): Unit = {

        val providerStateFunc = given.flatMap( g => setupProviderState).getOrElse({ _ : String => true})


        val (verifySettings, arguments) = sourceType match {
          case pactAsJsonString(json) =>
            val tmp = File.createTempFile("tmp_pact_", ".json")

            val fileWriter = new FileWriter(tmp, true)

            val bw = new BufferedWriter(fileWriter)
            bw.write(json)
            bw.close()

            (
              PactVerifySettings(
                providerStates = providerStateFunc,
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
                strictMode = strict,
                clientTimeout = clientTimeoutInSeconds
              )
            )

          case loadFromLocal(path) =>
            (
              PactVerifySettings(
                providerStates = providerStateFunc,
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
                strictMode = strict,
                clientTimeout = clientTimeoutInSeconds
              )
            )

          case pactBroker(url, providerName, consumerNames) =>
            (
              PactVerifySettings(
                providerStates = providerStateFunc,
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
                strictMode = strict,
                clientTimeout = clientTimeoutInSeconds
              )
            )

          case pactBrokerWithVersion(url, version, providerName, consumerNames) =>
            (
              PactVerifySettings(
                providerStates = providerStateFunc,
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
                strictMode = strict,
                clientTimeout = clientTimeoutInSeconds
              )
            )
        }

        val v = Verifier.verify(LocalPactFileLoader.loadPactFiles, verifySettings)

        if(v(arguments)) Unit else throw new ScalaPactVerifyFailed
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

    val defaultClientTimeoutInSeconds: Int = 2

    def apply(port: Int): VerifyTargetConfig = VerifyTargetConfig("http", "localhost", port, defaultClientTimeoutInSeconds)
    def apply(port: Int, clientTimeoutInSeconds: Int): VerifyTargetConfig = VerifyTargetConfig("http", "localhost", port, clientTimeoutInSeconds)
    def apply(host: String, port: Int): VerifyTargetConfig = VerifyTargetConfig("http", host, port, defaultClientTimeoutInSeconds)
    def apply(host: String, port: Int, clientTimeoutInSeconds: Int): VerifyTargetConfig = VerifyTargetConfig("http", host, port, clientTimeoutInSeconds)

    def fromUrl(url: String): Option[VerifyTargetConfig] = fromUrl(url, defaultClientTimeoutInSeconds)
    def fromUrl(url: String, clientTimeoutInSeconds: Int): Option[VerifyTargetConfig] = {
      try {
        val pattern = """^([a-z]+):\/\/([a-z0-9\.\-_]+):(\d+).*""".r
        val pattern(protocol, host, port) = url.toLowerCase

        VerifyTargetConfig(protocol, host, Helpers.safeStringToInt(port).getOrElse(80), clientTimeoutInSeconds)
      } catch {
        case e: Throwable =>
          println("Could not parse url '" + url + "', expected something like: http://localhost:80 (must specify the port!)")
          None
      }
    }

  }
  case class VerifyTargetConfig(protocol: String, host: String, port: Int, clientTimeoutInSeconds: Int) {
    def withProtocol(protocol: String): VerifyTargetConfig = this.copy(protocol = protocol)
    def withHost(host: String): VerifyTargetConfig = this.copy(host = host)
    def withPort(port: Int): VerifyTargetConfig = this.copy(port = port)
    def withClientTimeoutInSeconds(clientTimeoutInSeconds: Int): VerifyTargetConfig = this.copy(clientTimeoutInSeconds = clientTimeoutInSeconds)
  }

}
