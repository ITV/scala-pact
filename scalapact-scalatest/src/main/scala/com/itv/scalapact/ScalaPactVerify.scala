package com.itv.scalapact

import java.io.{BufferedWriter, File, FileWriter}

import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.IScalaPactHttpClientBuilder
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.settings.{ConsumerVerifySettings, LocalPactVerifySettings, PactsForVerificationSettings, PendingPactSettings, ScalaPactSettings}
import com.itv.scalapact.shared.utils.{Helpers, PactLogger}
import com.itv.scalapactcore.verifier.Verifier

import scala.concurrent.duration._
import scala.util.Try

trait ScalaPactVerifyDsl {
  private val defaultClientTimeout: Duration = 2.seconds

  object verifyPact {
    def withPactSource(
        sourceType: PactSourceType
    ): ScalaPactVerifyProviderStates =
      new ScalaPactVerifyProviderStates(sourceType)

    class ScalaPactVerifyProviderStates(sourceType: PactSourceType) {
      def setupProviderState(given: String)(setupProviderState: SetupProviderState): ScalaPactVerifyRunner =
        ScalaPactVerifyRunner(sourceType, given, setupProviderState)
      def noSetupRequired: ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, None, None)
    }

    class ScalaPactVerifyRunner(
        sourceType: PactSourceType,
        given: Option[String],
        setupProviderState: Option[SetupProviderState]
    ) {
      def runStrictVerificationAgainst(
          port: Int
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification("http", "localhost", port, defaultClientTimeout, strict = true)

      def runStrictVerificationAgainst(
          port: Int,
          clientTimeout: Duration
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification("http", "localhost", port, clientTimeout, strict = true)

      def runStrictVerificationAgainst(
          host: String,
          port: Int
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification("http", host, port, defaultClientTimeout, strict = true)

      def runStrictVerificationAgainst(host: String, port: Int, clientTimeout: Duration)(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit = doVerification("http", host, port, clientTimeout, strict = true)

      def runStrictVerificationAgainst(protocol: String, host: String, port: Int)(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification(protocol, host, port, defaultClientTimeout, strict = true)

      def runStrictVerificationAgainst(
          target: VerifyTargetConfig
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification(target.protocol, target.host, target.port, target.clientTimeout, strict = true)

      def runVerificationAgainst(
          port: Int
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification("http", "localhost", port, defaultClientTimeout, strict = false)

      def runVerificationAgainst(
          port: Int,
          clientTimeout: Duration
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification("http", "localhost", port, clientTimeout, strict = false)

      def runVerificationAgainst(
          host: String,
          port: Int
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification("http", host, port, defaultClientTimeout, strict = false)

      def runVerificationAgainst(host: String, port: Int, clientTimeout: Duration)(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification("http", host, port, clientTimeout, strict = false)

      def runVerificationAgainst(protocol: String, host: String, port: Int)(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification(protocol, host, port, defaultClientTimeout, strict = false)

      def runVerificationAgainst(protocol: String, host: String, port: Int, clientTimeout: Duration)(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification(protocol, host, port, clientTimeout, strict = false)

      def runVerificationAgainst(
          target: VerifyTargetConfig
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit =
        doVerification(target.protocol, target.host, target.port, target.clientTimeout, strict = false)

      private def doVerification(
          protocol: String,
          host: String,
          port: Int,
          clientTimeout: Duration,
          strict: Boolean
      )(implicit
          pactReader: IPactReader,
          pactWriter: IPactWriter,
          httpClientBuilder: IScalaPactHttpClientBuilder
      ): Unit = {

        val providerStateFunc: SetupProviderState =
          given
            .flatMap(_ => setupProviderState)
            .getOrElse(_ => ProviderStateResult(true))

        def makeScalaPactSettings(
            localPactFilePath: Option[String],
            publishResultsEnabled: Option[BrokerPublishData]
        ): ScalaPactSettings = ScalaPactSettings(
          host = Some(host),
          protocol = Some(protocol),
          port = Some(port),
          localPactFilePath = localPactFilePath,
          strictMode = Some(strict),
          clientTimeout = Some(clientTimeout),
          outputPath = None,
          publishResultsEnabled = publishResultsEnabled,
          pendingPactSettings = None
        )

        val (verifySettings, scalaPactSettings) = sourceType match {
          case pactAsJsonString(json) =>
            val tmp = File.createTempFile("tmp_pact_", ".json")

            val fileWriter = new FileWriter(tmp, true)

            val bw = new BufferedWriter(fileWriter)
            bw.write(json)
            bw.close()

            LocalPactVerifySettings(providerStateFunc) -> makeScalaPactSettings(Some(tmp.getAbsolutePath), None)

          case loadFromLocal(path) =>
            LocalPactVerifySettings(providerStateFunc) -> makeScalaPactSettings(Some(path), None)

          case pactBrokerUseLatest(
                url,
                providerName,
                consumerNames,
                publishResultsEnabled,
                pactBrokerAuthorization,
                pactBrokerClientTimeout
              ) =>
            ConsumerVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = url,
              providerName = providerName,
              versionedConsumerNames = consumerNames.map(VersionedConsumer.fromName),
              pactBrokerAuthorization = pactBrokerAuthorization,
              pactBrokerClientTimeout = pactBrokerClientTimeout,
              sslContextName = None
            ) ->
              makeScalaPactSettings(None, publishResultsEnabled)

          case pactBrokerWithTags(
                url,
                providerName,
                publishResultsEnabled,
                consumersWithTags,
                pactBrokerAuthorization,
                pactBrokerClientTimeout
              ) =>
            ConsumerVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = url,
              providerName = providerName,
              versionedConsumerNames = consumersWithTags.flatMap(_.toVersionedConsumers),
              pactBrokerAuthorization = pactBrokerAuthorization,
              pactBrokerClientTimeout = pactBrokerClientTimeout,
              sslContextName = None
            ) ->
              makeScalaPactSettings(None, publishResultsEnabled)

          case pactBrokerWithVersion(
                url,
                version,
                providerName,
                consumerNames,
                publishResultsEnabled,
                pactBrokerAuthorization,
                pactBrokerClientTimeout
              ) =>
            ConsumerVerifySettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = url,
              providerName = providerName,
              versionedConsumerNames = consumerNames.map(c => VersionedConsumer(c, version)),
              pactBrokerAuthorization = pactBrokerAuthorization,
              pactBrokerClientTimeout = pactBrokerClientTimeout,
              sslContextName = None
            ) ->
              makeScalaPactSettings(None, publishResultsEnabled)

          case pactBrokerWithVersionSelectors(
                url,
                providerName,
                consumerVersionSelectors,
                providerVersionTags,
                pendingPactSettings,
                publishResultsEnabled,
                pactBrokerAuthorization,
                pactBrokerClientTimeout
              ) =>
            PactsForVerificationSettings(
              providerStates = providerStateFunc,
              pactBrokerAddress = url,
              providerName = providerName,
              pendingPactSettings = pendingPactSettings,
              consumerVersionSelectors = consumerVersionSelectors,
              providerVersionTags = providerVersionTags,
              pactBrokerAuthorization = pactBrokerAuthorization,
              pactBrokerClientTimeout = pactBrokerClientTimeout,
              sslContextName = None
            ) ->
              makeScalaPactSettings(None, publishResultsEnabled)
        }

        if (Verifier.apply.verify(verifySettings, scalaPactSettings)) () else throw new ScalaPactVerifyFailed
      }
    }

    object ScalaPactVerifyRunner {
      def apply(
          sourceType: PactSourceType,
          given: String,
          setupProviderState: SetupProviderState
      ): ScalaPactVerifyRunner = new ScalaPactVerifyRunner(sourceType, Some(given), Some(setupProviderState))
    }

  }

  sealed trait PactSourceType

  case class loadFromLocal(path: String)    extends PactSourceType
  case class pactAsJsonString(json: String) extends PactSourceType

  case class pactBrokerUseLatest(
      url: String,
      provider: String,
      consumers: List[String],
      publishResultsEnabled: Option[BrokerPublishData],
      pactBrokerAuthorization: Option[PactBrokerAuthorization],
      pactBrokerClientTimeout: Option[Duration]
  ) extends PactSourceType {
    def withContractVersion(version: String): pactBrokerWithVersion =
      pactBrokerWithVersion(
        url,
        version,
        provider,
        consumers,
        publishResultsEnabled,
        pactBrokerAuthorization,
        pactBrokerClientTimeout
      )
  }

  object pactBrokerUseLatest {
    def apply(url: String, provider: String, consumers: List[String]): pactBrokerUseLatest =
      pactBrokerUseLatest(url, provider, consumers, None, None, None)
  }

  case class pactBrokerWithTags(
      url: String,
      provider: String,
      publishResultsEnabled: Option[BrokerPublishData],
      consumerNamesWithTags: List[TaggedConsumer],
      pactBrokerAuthorization: Option[PactBrokerAuthorization],
      pactBrokerClientTimeout: Option[Duration]
  ) extends PactSourceType

  object pactBrokerWithTags {
    def apply(url: String, provider: String, consumers: List[TaggedConsumer]): pactBrokerWithTags =
      pactBrokerWithTags(url, provider, None, consumers, None, None)
  }

  case class pactBrokerWithVersion(
      url: String,
      contractVersion: String,
      provider: String,
      consumers: List[String],
      publishResultsEnabled: Option[BrokerPublishData],
      pactBrokerAuthorization: Option[PactBrokerAuthorization],
      pactBrokerClientTimeout: Option[Duration]
  ) extends PactSourceType

  object pactBrokerWithVersion {
    def apply(url: String, contractVersion: String, provider: String, consumers: List[String]): pactBrokerWithVersion =
      pactBrokerWithVersion(url, contractVersion, provider, consumers, None, None, None)
  }

  case class pactBrokerWithVersionSelectors(
      url: String,
      provider: String,
      consumerVersionSelectors: List[ConsumerVersionSelector],
      providerVersionTags: List[String],
      pendingPactSettings: PendingPactSettings,
      publishResultsEnabled: Option[BrokerPublishData],
      pactBrokerAuthorization: Option[PactBrokerAuthorization],
      pactBrokerClientTimeout: Option[Duration]
  ) extends PactSourceType {
    def withPendingPactSettings(settings: PendingPactSettings): pactBrokerWithVersionSelectors =
      this.copy(pendingPactSettings = settings)
    def withBrokerClientTimeout(duration: Duration): pactBrokerWithVersionSelectors =
      this.copy(pactBrokerClientTimeout = Some(duration))
    def withPactBrokerAuth(auth: PactBrokerAuthorization): pactBrokerWithVersionSelectors =
      this.copy(pactBrokerAuthorization = Some(auth))
    def withPublishData(data: BrokerPublishData): pactBrokerWithVersionSelectors =
      this.copy(publishResultsEnabled = Some(data))
  }

  object pactBrokerWithVersionSelectors {
    def apply(
        url: String,
        provider: String,
        consumerVersionSelectors: List[ConsumerVersionSelector],
        providerVersionTags: List[String]
    ): pactBrokerWithVersionSelectors =
      pactBrokerWithVersionSelectors(
        url,
        provider,
        consumerVersionSelectors,
        providerVersionTags,
        PendingPactSettings.PendingDisabled,
        None,
        None,
        None
      )
  }

  class ScalaPactVerifyFailed extends Exception

  case class VerifyTargetConfig(protocol: String, host: String, port: Int, clientTimeout: Duration) {
    def withProtocol(protocol: String): VerifyTargetConfig             = this.copy(protocol = protocol)
    def withHost(host: String): VerifyTargetConfig                     = this.copy(host = host)
    def withPort(port: Int): VerifyTargetConfig                        = this.copy(port = port)
    def withClientTimeout(clientTimeout: Duration): VerifyTargetConfig = this.copy(clientTimeout = clientTimeout)

    @deprecated("Use `withClientTimeout` instead", "2.3.19")
    def withClientTimeoutInSeconds(clientTimeout: Duration): VerifyTargetConfig =
      this.copy(clientTimeout = clientTimeout)
  }

  object VerifyTargetConfig {
    def apply(port: Int): VerifyTargetConfig = VerifyTargetConfig("http", "localhost", port, defaultClientTimeout)
    def apply(port: Int, clientTimeout: Duration): VerifyTargetConfig =
      VerifyTargetConfig("http", "localhost", port, clientTimeout)
    def apply(host: String, port: Int): VerifyTargetConfig =
      VerifyTargetConfig("http", host, port, defaultClientTimeout)
    def apply(protocol: String, host: String, port: Int): VerifyTargetConfig =
      VerifyTargetConfig(protocol, host, port, defaultClientTimeout)
    def apply(host: String, port: Int, clientTimeout: Duration): VerifyTargetConfig =
      VerifyTargetConfig("http", host, port, clientTimeout)

    def fromUrl(url: String): Option[VerifyTargetConfig] = fromUrl(url, defaultClientTimeout)
    def fromUrl(url: String, clientTimeout: Duration): Option[VerifyTargetConfig] =
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

object ScalaPactVerify extends ScalaPactVerifyDsl
