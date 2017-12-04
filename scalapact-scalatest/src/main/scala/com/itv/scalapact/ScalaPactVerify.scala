package com.itv.scalapact

import com.itv.scalapactcore.common.LocalPactFileLoader
import com.itv.scalapactcore.verifier.{PactVerifySettings, Verifier, VersionedConsumer}
import java.io.{BufferedWriter, File, FileWriter}

import com.itv.scalapact.shared.{Helpers, ScalaPactSettings, SslContextMap}
import com.itv.scalapactcore.common.PactReaderWriter._

import scala.concurrent.duration._
import scala.language.implicitConversions

object ScalaPactVerify {
  implicit def toOption[A](a: A): Option[A] = Option(a)

  object verifyPact {

    def withPactSource(sourceType: PactSourceType)(implicit sslContextMap: SslContextMap): ScalaPactVerifyProviderStates = new ScalaPactVerifyProviderStates(sourceType)

    class ScalaPactVerifyProviderStates(sourceType: PactSourceType) (implicit sslContextMap: SslContextMap){
      def setupProviderState(given: String)(setupProviderState: String => Boolean): ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, given, setupProviderState)
      def noSetupRequired: ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, None, None)
    }

    class ScalaPactVerifyRunner(sourceType: PactSourceType, given: Option[String], setupProviderState: Option[String => Boolean])(implicit sslContextMap: SslContextMap) {

      def runStrictVerificationAgainst(port: Int): Unit = doVerification("http", "localhost", port, VerifyTargetConfig.defaultClientTimeout, strict = true)
      def runStrictVerificationAgainst(port: Int, clientTimeout: Duration): Unit = doVerification("http", "localhost", port, clientTimeout, strict = true)
      def runStrictVerificationAgainst(host: String, port: Int): Unit = doVerification("http", host, port, VerifyTargetConfig.defaultClientTimeout, strict = true)
      def runStrictVerificationAgainst(host: String, port: Int, clientTimeout: Duration): Unit = doVerification("http", host, port, clientTimeout, strict = true)
      def runStrictVerificationAgainst(protocol: String, host: String, port: Int): Unit = doVerification(protocol, host, port, VerifyTargetConfig.defaultClientTimeout, strict = true)
      def runStrictVerificationAgainst(target: VerifyTargetConfig): Unit = doVerification(target.protocol, target.host, target.port, target.clientTimeout, strict = true)

      def runVerificationAgainst(port: Int): Unit = doVerification("http", "localhost", port, VerifyTargetConfig.defaultClientTimeout, strict = false)
      def runVerificationAgainst(port: Int, clientTimeout: Duration): Unit = doVerification("http", "localhost", port, clientTimeout, strict = false)
      def runVerificationAgainst(host: String, port: Int): Unit = doVerification("http", host, port, VerifyTargetConfig.defaultClientTimeout, strict = false)
      def runVerificationAgainst(host: String, port: Int, clientTimeout: Duration): Unit = doVerification("http", host, port, clientTimeout, strict = false)
      def runVerificationAgainst(protocol: String, host: String, port: Int): Unit = doVerification(protocol, host, port, VerifyTargetConfig.defaultClientTimeout, strict = false)
      def runVerificationAgainst(protocol: String, host: String, port: Int, clientTimeout: Duration): Unit = doVerification(protocol, host, port, clientTimeout, strict = false)
      def runVerificationAgainst(target: VerifyTargetConfig): Unit = doVerification(target.protocol, target.host, target.port, target.clientTimeout, strict = false)

      private def doVerification(protocol: String, host: String, port: Int, clientTimeout: Duration, strict: Boolean): Unit = {

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
              ScalaPactSettings(
                host = host,
                protocol = protocol,
                port = port,
                localPactFilePath = tmp.getAbsolutePath(),
                strictMode = strict,
                clientTimeout = Option(clientTimeout),
                outputPath = None
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
              ScalaPactSettings(
                host = host,
                protocol = protocol,
                port = port,
                localPactFilePath = path,
                strictMode = strict,
                clientTimeout = Option(clientTimeout),
                outputPath = None
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
              ScalaPactSettings(
                host = host,
                protocol = protocol,
                port = port,
                localPactFilePath = None,
                strictMode = strict,
                clientTimeout = Option(clientTimeout),
                outputPath = None
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
              ScalaPactSettings(
                host = host,
                protocol = protocol,
                port = port,
                localPactFilePath = None,
                strictMode = strict,
                clientTimeout = Option(clientTimeout),
                outputPath = None
              )
            )
        }

        val v = Verifier.verify(LocalPactFileLoader.loadPactFiles(pactReader)(true), verifySettings)

        if(v(arguments)) () else throw new ScalaPactVerifyFailed
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

    val defaultClientTimeout: Duration = Duration(2, SECONDS)

    def apply(port: Int): VerifyTargetConfig = VerifyTargetConfig("http", "localhost", port, defaultClientTimeout)
    def apply(port: Int, clientTimeout: Duration): VerifyTargetConfig = VerifyTargetConfig("http", "localhost", port, clientTimeout)
    def apply(host: String, port: Int): VerifyTargetConfig = VerifyTargetConfig("http", host, port, defaultClientTimeout)
    def apply(host: String, port: Int, clientTimeout: Duration): VerifyTargetConfig = VerifyTargetConfig("http", host, port, clientTimeout)

    def fromUrl(url: String): Option[VerifyTargetConfig] = fromUrl(url, defaultClientTimeout)
    def fromUrl(url: String, clientTimeout: Duration): Option[VerifyTargetConfig] = {
      try {
        val pattern = """^([a-z]+):\/\/([a-z0-9\.\-_]+):(\d+).*""".r
        val pattern(protocol, host, port) = url.toLowerCase

        VerifyTargetConfig(protocol, host, Helpers.safeStringToInt(port).getOrElse(80), clientTimeout)
      } catch {
        case e: Throwable =>
          println("Could not parse url '" + url + "', expected something like: http://localhost:80 (must specify the port!)")
          None
      }
    }

  }
  case class VerifyTargetConfig(protocol: String, host: String, port: Int, clientTimeout: Duration) {
    def withProtocol(protocol: String): VerifyTargetConfig = this.copy(protocol = protocol)
    def withHost(host: String): VerifyTargetConfig = this.copy(host = host)
    def withPort(port: Int): VerifyTargetConfig = this.copy(port = port)
    def withClientTimeoutInSeconds(clientTimeout: Duration): VerifyTargetConfig = this.copy(clientTimeout = clientTimeout)
  }

}
