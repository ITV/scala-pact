package com.itv.scalapact.model

import com.itv.scalapact.shared.IPactStubber
import com.itv.scalapact.{ScalaPactMock, ScalaPactMockConfig}
import com.itv.scalapact.shared.utils.Maps._
import com.itv.scalapact.shared.http.{IScalaPactHttpClient, IScalaPactHttpClientBuilder, SslContextMap}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}

import scala.concurrent.duration._

class ScalaPactDescription(strict: Boolean,
                           consumer: String,
                           provider: String,
                           sslContextName: Option[String],
                           interactions: List[ScalaPactInteraction]) {

  /**
    * Adds interactions to the Pact. Interactions should be created using the helper object 'interaction'
    *
    * @param interaction [ScalaPactInteraction] definition
    * @return [ScalaPactDescription] to allow the builder to continue
    */
  def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription =
    new ScalaPactDescription(strict, consumer, provider, sslContextName, interactions :+ interaction)

  def addSslContextForServer(name: String): ScalaPactDescription =
    new ScalaPactDescription(strict, consumer, provider, Some(name), interactions)

  def runConsumerTest[A](test: ScalaPactMockConfig => A)(implicit options: ScalaPactOptions,
                                                               sslContextMap: SslContextMap,
                                                               pactReader: IPactReader,
                                                               pactWriter: IPactWriter,
                                                               httpClientBuilder: IScalaPactHttpClientBuilder,
                                                               pactStubber: IPactStubber): A = {
    implicit val client: IScalaPactHttpClient =
      httpClientBuilder.build(2.seconds, sslContextName, 1)
    ScalaPactMock.runConsumerIntegrationTest(strict)(
      finalise
    )(test)
  }

  private def finalise(implicit options: ScalaPactOptions): ScalaPactDescriptionFinal =
    ScalaPactDescriptionFinal(
      consumer,
      provider,
      sslContextName,
      interactions.map(i => i.finalise),
      options
    )
}

final case class ScalaPactDescriptionFinal(consumer: String,
                                     provider: String,
                                     serverSslContextName: Option[String],
                                     interactions: List[ScalaPactInteractionFinal],
                                     options: ScalaPactOptions) {
  def withHeaderForSsl: ScalaPactDescriptionFinal =
    copy(
      interactions = interactions.map(
        i =>
          i.copy(
            request = i.request
              .copy(headers = i.request.headers addOpt (SslContextMap.sslContextHeaderName -> i.sslContextName))
          )
      )
    )
}