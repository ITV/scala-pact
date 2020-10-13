package com.itv.scalapact

import com.itv.scalapactcore.common.LocalPactFileLoader
import com.itv.scalapactcore.verifier.Verifier
import java.io.{BufferedWriter, File, FileWriter}

import com.itv.scalapact.shared._

import scala.concurrent.duration._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter, IScalaPactHttpClient}
import com.itv.scalapact.shared.ProviderStateResult
import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState

import scala.util.Try

object ScalaPactVerify {
  private val defaultClientTimeout: Duration = 2.seconds

  object verifyPact {
    def withPactSource(
        sourceType: PactSourceType
    )(implicit sslContextMap: SslContextMap): ScalaPactVerifyProviderStates =
      new ScalaPactVerifyProviderStates(sourceType)

    class ScalaPactVerifyProviderStates(sourceType: PactSourceType)(implicit sslContextMap: SslContextMap) {
      def setupProviderState(given: String)(setupProviderState: SetupProviderState): ScalaPactVerifyRunner =
        ScalaPactVerifyRunner(sourceType, given, setupProviderState)
      def noSetupRequired: ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, None, None)
    }

    class ScalaPactVerifyRunner(
        sourceType: PactSourceType,
        given: Option[String],
        setupProviderState: Option[SetupProviderState]
    )(implicit sslContextMap: SslContextMap) {

      def runStrictVerificationAgainst[F[_]](
          port: Int
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit =
        doVerification("http", "localhost", port, defaultClientTimeout, strict = true)
      def runStrictVerificationAgainst[F[_]](
          port: Int,
          clientTimeout: Duration
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit =
        doVerification("http", "localhost", port, clientTimeout, strict = true)
      def runStrictVerificationAgainst[F[_]](
          host: String,
          port: Int
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit =
        doVerification("http", host, port, defaultClientTimeout, strict = true)
      def runStrictVerificationAgainst[F[_]](host: String, port: Int, clientTimeout: Duration)(
          implicit pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClient: IScalaPactHttpClient[F],
          publisher: IResultPublisher
      ): Unit = doVerification("http", host, port, clientTimeout, strict = true)
      def runStrictVerificationAgainst[F[_]](protocol: String, host: String, port: Int)(
          implicit pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClient: IScalaPactHttpClient[F],
          publisher: IResultPublisher
      ): Unit =
        doVerification(protocol, host, port, defaultClientTimeout, strict = true)
      def runStrictVerificationAgainst[F[_]](
          target: VerifyTargetConfig
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit =
        doVerification(target.protocol, target.host, target.port, target.clientTimeout, strict = true)
      def runVerificationAgainst[F[_]](
          port: Int
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit =
        doVerification("http", "localhost", port, defaultClientTimeout, strict = false)
      def runVerificationAgainst[F[_]](
          port: Int,
          clientTimeout: Duration
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit =
        doVerification("http", "localhost", port, clientTimeout, strict = false)
      def runVerificationAgainst[F[_]](
          host: String,
          port: Int
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit =
        doVerification("http", host, port, defaultClientTimeout, strict = false)
      def runVerificationAgainst[F[_]](host: String, port: Int, clientTimeout: Duration)(
          implicit pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClient: IScalaPactHttpClient[F],
          publisher: IResultPublisher
      ): Unit =
        doVerification("http", host, port, clientTimeout, strict = false)
      def runVerificationAgainst[F[_]](protocol: String, host: String, port: Int)(
          implicit pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClient: IScalaPactHttpClient[F],
          publisher: IResultPublisher
      ): Unit =
        doVerification(protocol, host, port, defaultClientTimeout, strict = false)
      def runVerificationAgainst[F[_]](protocol: String, host: String, port: Int, clientTimeout: Duration)(
          implicit pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClient: IScalaPactHttpClient[F],
          publisher: IResultPublisher
      ): Unit =
        doVerification(protocol, host, port, clientTimeout, strict = false)
      def runVerificationAgainst[F[_]](
          target: VerifyTargetConfig
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit =
        doVerification(target.protocol, target.host, target.port, target.clientTimeout, strict = false)

      private def doVerification[F[_]](
          protocol: String,
          host: String,
          port: Int,
          clientTimeout: Duration,
          strict: Boolean
      )(implicit pactReader: IPactReader, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F], publisher: IResultPublisher): Unit = {

        val providerStateFunc: SetupProviderState =
          given
            .flatMap(_ => setupProviderState)
            .getOrElse(_ => ProviderStateResult(true))

        def makeScalaPactSettings(localPactFilePath: Option[String], publishResultsEnabled: Option[BrokerPublishData]): ScalaPactSettings = ScalaPactSettings(
          host = Some(host),
          protocol = Some(protocol),
          port = Some(port),
          localPactFilePath = localPactFilePath,
          strictMode = Some(strict),
          clientTimeout = Some(clientTimeout),
          outputPath = None,
          publishResultsEnabled = publishResultsEnabled
        )


        val (verifySettings, scalaPactSettings) = sourceType match {
          case pactAsJsonString(json) =>
            val tmp = File.createTempFile("tmp_pact_", ".json")

            val fileWriter = new FileWriter(tmp, true)

            val bw = new BufferedWriter(fileWriter)
            bw.write(json)
            bw.close()

            PactVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = "",
              projectVersion = "",
              providerName = "",
              consumerNames = Nil,
              taggedConsumerNames = Nil,
              versionedConsumerNames = Nil,
              consumerVersionSelectors = Nil,
              providerVersionTags = Nil,
              pactBrokerAuthorization = None
            ) ->
              makeScalaPactSettings(Some(tmp.getAbsolutePath), None)

          case loadFromLocal(path) =>
            PactVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = "",
              projectVersion = "",
              providerName = "",
              consumerNames = Nil,
              taggedConsumerNames = Nil,
              versionedConsumerNames = Nil,
              consumerVersionSelectors = Nil,
              providerVersionTags = Nil,
              pactBrokerAuthorization = None
            ) ->
              makeScalaPactSettings(Some(path), None)

          case pactBrokerUseLatest(url, providerName, consumerNames, publishResultsEnabled, pactBrokerAuthorization) =>
            PactVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = url,
              projectVersion = "",
              providerName = providerName,
              consumerNames = consumerNames,
              taggedConsumerNames = Nil,
              versionedConsumerNames = Nil,
              consumerVersionSelectors = Nil,
              providerVersionTags = Nil,
              pactBrokerAuthorization = pactBrokerAuthorization
            ) ->
            makeScalaPactSettings(None, publishResultsEnabled)

          case pactBrokerWithTags(
              url,
              providerName,
          publishResultsEnabled,
              consumersWithTags,
              pactBrokerAuthorization
              ) =>
            PactVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = url,
              projectVersion = "",
              providerName = providerName,
              consumerNames = Nil,
              taggedConsumerNames = consumersWithTags,
              versionedConsumerNames = Nil,
              consumerVersionSelectors = Nil,
              providerVersionTags = Nil,
              pactBrokerAuthorization = pactBrokerAuthorization
            ) ->
              makeScalaPactSettings(None, publishResultsEnabled)

          case pactBrokerWithVersion(
              url,
              version,
              providerName,
              consumerNames,
              publishResultsEnabled,
              pactBrokerAuthorization
              ) =>
            PactVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = url,
              projectVersion = "",
              providerName = providerName,
              consumerNames = Nil,
              taggedConsumerNames = Nil,
              versionedConsumerNames = consumerNames.map(c => VersionedConsumer(c, version)),
              consumerVersionSelectors = Nil,
              providerVersionTags = Nil,
              pactBrokerAuthorization = pactBrokerAuthorization
            ) ->
            makeScalaPactSettings(None, publishResultsEnabled)

          case pactBrokerWithVersionSelectors(
              url,
              providerName,
              consumerVersionSelectors,
              providerVersionTags,
              publishResultsEnabled,
              pactBrokerAuthorization
              ) =>
            PactVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = url,
              projectVersion = "",
              providerName = providerName,
              consumerNames = Nil,
              taggedConsumerNames = Nil,
              versionedConsumerNames = Nil,
              consumerVersionSelectors = consumerVersionSelectors,
              providerVersionTags = providerVersionTags,
              pactBrokerAuthorization = pactBrokerAuthorization
            ) ->
            makeScalaPactSettings(None, publishResultsEnabled)
        }

        val v: ScalaPactSettings => Boolean = Verifier[F].verify(LocalPactFileLoader.loadPactFiles(pactReader)(true), verifySettings)

        if (v(scalaPactSettings)) () else throw new ScalaPactVerifyFailed
      }
    }

    object ScalaPactVerifyRunner {
      def apply(
                 sourceType: PactSourceType,
                 given: String,
                 setupProviderState: SetupProviderState
               )(implicit sslContextMap: SslContextMap): ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, Some(given), Some(setupProviderState))
    }

  }

  sealed trait PactSourceType

  case class loadFromLocal(path: String) extends PactSourceType

  case class pactBrokerUseLatest(
      url: String,
      provider: String,
      consumers: List[String],
      publishResultsEnabled: Option[BrokerPublishData],
      pactBrokerAuthorization: Option[PactBrokerAuthorization]
  ) extends PactSourceType {
    def withContractVersion(version: String): pactBrokerWithVersion =
      pactBrokerWithVersion(url, version, provider, consumers, publishResultsEnabled, pactBrokerAuthorization)
  }

  case class pactBrokerWithTags(
      url: String,
      provider: String,
      publishResultsEnabled: Option[BrokerPublishData],
      consumerNamesWithTags: List[TaggedConsumer],
      pactBrokerAuthorization: Option[PactBrokerAuthorization]
  ) extends PactSourceType

  case class pactBrokerWithVersion(
      url: String,
      contractVersion: String,
      provider: String,
      consumers: List[String],
      publishResultsEnabled: Option[BrokerPublishData],
      pactBrokerAuthorization: Option[PactBrokerAuthorization]
  ) extends PactSourceType

  case class pactBrokerWithVersionSelectors(
      url: String,
      provider: String,
      consumerVersionSelectors: List[ConsumerVersionSelector],
      providerVersionTags: List[String],
      publishResultsEnabled: Option[BrokerPublishData],
      pactBrokerAuthorization: Option[PactBrokerAuthorization]
  ) extends PactSourceType

  case class pactAsJsonString(json: String) extends PactSourceType

  class ScalaPactVerifyFailed extends Exception

  case class VerifyTargetConfig(protocol: String, host: String, port: Int, clientTimeout: Duration) {
    def withProtocol(protocol: String): VerifyTargetConfig = this.copy(protocol = protocol)
    def withHost(host: String): VerifyTargetConfig         = this.copy(host = host)
    def withPort(port: Int): VerifyTargetConfig            = this.copy(port = port)
    def withClientTimeout(clientTimeout: Duration): VerifyTargetConfig = this.copy(clientTimeout = clientTimeout)

    @deprecated("Use `withClientTimeout` instead", "2.3.19")
    def withClientTimeoutInSeconds(clientTimeout: Duration): VerifyTargetConfig =
      this.copy(clientTimeout = clientTimeout)
  }

  object VerifyTargetConfig {
    def apply(port: Int): VerifyTargetConfig = VerifyTargetConfig("http", "localhost", port, defaultClientTimeout)
    def apply(port: Int, clientTimeout: Duration): VerifyTargetConfig = VerifyTargetConfig("http", "localhost", port, clientTimeout)
    def apply(host: String, port: Int): VerifyTargetConfig = VerifyTargetConfig("http", host, port, defaultClientTimeout)
    def apply(protocol: String, host: String, port: Int): VerifyTargetConfig = VerifyTargetConfig(protocol, host, port, defaultClientTimeout)
    def apply(host: String, port: Int, clientTimeout: Duration): VerifyTargetConfig = VerifyTargetConfig("http", host, port, clientTimeout)

    def fromUrl(url: String): Option[VerifyTargetConfig] = fromUrl(url, defaultClientTimeout)
    def fromUrl(url: String, clientTimeout: Duration): Option[VerifyTargetConfig] = {
      Try {
        val pattern                       = """^([a-z]+)://([a-z0-9.\-_]+):(\d+).*""".r
        val pattern(protocol, host, port) = url.toLowerCase
        Some(VerifyTargetConfig(protocol, host, Helpers.safeStringToInt(port).getOrElse(80), clientTimeout))
      }.getOrElse {
        PactLogger.error(
          "Could not parse url '" + url + "', expected something like: http://localhost:80 (must specify the port!)"
        )
        None
      }
    }

  }
}
