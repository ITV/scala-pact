package com.itv.scalapactcore.verifier

import com.itv.scalapact.shared.ProviderStateResult.SetupProviderState
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactWriter, IResultPublisherBuilder, IScalaPactHttpClientBuilder}
import com.itv.scalapactcore.common._

class Verifier[F[_]](
  pactBrokerClient: PactBrokerClient[F])(
  implicit pactReader: IPactReader,
  httpClientBuilder: IScalaPactHttpClientBuilder[F],
  publisherBuilder: IResultPublisherBuilder) {

  def verify(pactVerifySettings: PactVerifySettings, scalaPactSettings: ScalaPactSettings): Boolean = {
    val localVerificationClient = httpClientBuilder.build(scalaPactSettings.giveClientTimeout, None)
    pactVerifySettings match {
      case settings: LocalPactVerifySettings =>
        new LocalPactVerifier[F](localVerificationClient).verify(settings, scalaPactSettings)
      case settings: PrePactsForVerificationSettings =>
        new PrePactsForVerificationVerifier[F](pactBrokerClient, localVerificationClient).verify(settings, scalaPactSettings)
      case settings: PactsForVerificationSettings =>
        new PactsForVerificationVerifier[F](pactBrokerClient, localVerificationClient).verify(settings, scalaPactSettings)
    }
  }
}

object Verifier {
  def apply[F[_]](implicit pactReader: IPactReader,
                  pactWriter: IPactWriter,
                  httpClient: IScalaPactHttpClientBuilder[F],
                  publisher: IResultPublisherBuilder): Verifier[F] =
    new Verifier[F](new PactBrokerClient[F])
}

case class ProviderStateFailure(key: String) extends Exception()

case class ProviderState(key: String, f: SetupProviderState)

//TODO remove
case class ValidatedDetails(validatedAddress: ValidPactBrokerAddress,
                            providerName: String,
                            consumerName: String,
                            consumerVersion: String)

object ValidatedDetails {

  def buildFrom(consumerName: String,
                providerName: String,
                pactBrokerAddress: String,
                consumerVersion: String): Either[String, ValidatedDetails] = {
    for {
      consumer <- Helpers.urlEncode(consumerName)
      provider <- Helpers.urlEncode(providerName)
      validatedAddress <- PactBrokerAddressValidation.checkPactBrokerAddress(pactBrokerAddress)
    } yield ValidatedDetails(validatedAddress, provider, consumer, consumerVersion)
  }
}
