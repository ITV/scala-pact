package com.itv.scalapactcore.verifier

import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.IScalaPactHttpClientBuilder
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.settings.{ConsumerVerifySettings, LocalPactVerifySettings, PactVerifySettings, PactsForVerificationSettings, ScalaPactSettings}
import com.itv.scalapactcore.common.PactBrokerClient

class Verifier(pactBrokerClient: PactBrokerClient)(implicit
    pactReader: IPactReader,
    httpClientBuilder: IScalaPactHttpClientBuilder
) {

  def verify(pactVerifySettings: PactVerifySettings, scalaPactSettings: ScalaPactSettings): Boolean = {
    val localVerificationClient = httpClientBuilder.build(scalaPactSettings.giveClientTimeout, None, 1)
    pactVerifySettings match {
      case settings: LocalPactVerifySettings =>
        new LocalPactVerifier(localVerificationClient).verify(settings, scalaPactSettings)
      case settings: ConsumerVerifySettings =>
        new PrePactsForVerificationVerifier(pactBrokerClient, localVerificationClient)
          .verify(settings, scalaPactSettings)
      case settings: PactsForVerificationSettings =>
        new PactsForVerificationVerifier(pactBrokerClient, localVerificationClient).verify(settings, scalaPactSettings)
    }
  }
}

object Verifier {
  def apply(implicit
      pactReader: IPactReader,
      pactWriter: IPactWriter,
      httpClient: IScalaPactHttpClientBuilder
  ): Verifier =
    new Verifier(new PactBrokerClient)
}

case class ProviderStateFailure(key: String) extends Exception()

case class ProviderState(key: String, f: SetupProviderState)
