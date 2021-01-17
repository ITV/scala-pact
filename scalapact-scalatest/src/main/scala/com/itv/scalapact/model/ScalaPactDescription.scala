package com.itv.scalapact.model

import com.itv.scalapact.shared.IPactStubber
import com.itv.scalapact.{ScalaPactContractWriter, ScalaPactMock, ScalaPactMockConfig, ScalaPactMockServer}
import com.itv.scalapact.shared.utils.Maps._
import com.itv.scalapact.shared.http.{IScalaPactHttpClient, IScalaPactHttpClientBuilder, SslContextMap}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}

import scala.concurrent.duration._

class ScalaPactDescription(
    strict: Boolean,
    consumer: String,
    provider: String,
    sslContextName: Option[String],
    interactions: List[ScalaPactInteraction]
) {

  /** Adds interactions to the Pact. Interactions should be created using the helper object 'interaction'
    *
    * @param interaction [ScalaPactInteraction] definition
    * @return [ScalaPactDescription] to allow the builder to continue
    */
  def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription =
    new ScalaPactDescription(strict, consumer, provider, sslContextName, interaction :: interactions)

  def addSslContextForServer(name: String): ScalaPactDescription =
    new ScalaPactDescription(strict, consumer, provider, Some(name), interactions)

  def runConsumerTest[A](test: ScalaPactMockConfig => A)(implicit
      options: ScalaPactOptions,
      sslContextMap: SslContextMap,
      pactReader: IPactReader,
      pactWriter: IPactWriter,
      httpClientBuilder: IScalaPactHttpClientBuilder,
      pactStubber: IPactStubber
  ): A = {
    implicit val client: IScalaPactHttpClient =
      httpClientBuilder.build(2.seconds, sslContextName, 1)
    ScalaPactMock.runConsumerIntegrationTest(strict)(finalise)(test)
  }

  /** Starts the `ScalaPactMockServer`, which tests can then be run against. It is important that the server be
    * shutdown when no longer needed by invoking `stop()`.
    */
  def startServer()(implicit
      httpClientBuilder: IScalaPactHttpClientBuilder,
      options: ScalaPactOptions,
      pactReader: IPactReader,
      pactWriter: IPactWriter,
      pactStubber: IPactStubber
  ): ScalaPactMockServer = {
    implicit val client: IScalaPactHttpClient =
      httpClientBuilder.build(2.seconds, sslContextName, 1)
    val pactDescriptionFinal = finalise(options)
    val server               = ScalaPactMock.startServer(strict, pactDescriptionFinal)
    if (pactDescriptionFinal.options.writePactFiles) {
      ScalaPactContractWriter.writePactContracts(server.config.outputPath)(pactWriter)(
        pactDescriptionFinal.withHeaderForSsl
      )
    }
    server
  }

  /** Writes pacts described by this ScalaPactDescription to file without running any consumer tests
    */
  def writePactsToFile(implicit options: ScalaPactOptions, pactWriter: IPactWriter): Unit = {
    val pactDescription = finalise(options)
    ScalaPactContractWriter.writePactContracts(options.outputPath)(pactWriter)(pactDescription.withHeaderForSsl)
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

final case class ScalaPactDescriptionFinal(
    consumer: String,
    provider: String,
    serverSslContextName: Option[String],
    interactions: List[ScalaPactInteractionFinal],
    options: ScalaPactOptions
) {
  def withHeaderForSsl: ScalaPactDescriptionFinal =
    copy(
      interactions = interactions.map(i =>
        i.copy(
          request = i.request
            .copy(headers = i.request.headers addOpt (SslContextMap.sslContextHeaderName -> i.sslContextName))
        )
      )
    )
}
