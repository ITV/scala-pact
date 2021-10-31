package com.itv.scalapact

import com.itv.scalapact.model.ScalaPactDescriptionFinal
import com.itv.scalapact.shared.http.{HttpMethod, IScalaPactHttpClient, SimpleRequest, SslContextMap}
import com.itv.scalapact.shared.json.{IPactReader, IPactWriter}
import com.itv.scalapact.shared.utils.PactLogger
import com.itv.scalapact.shared.{IPactStubber, ScalaPactSettings}
import com.itv.scalapactcore.common.stubber.InteractionManager

private[scalapact] object ScalaPactMock {

  private def configuredTestRunner[A](
      pactDescription: ScalaPactDescriptionFinal
  )(config: ScalaPactMockConfig)(test: => ScalaPactMockConfig => A)(implicit pactWriter: IPactWriter): A = {

    if (pactDescription.options.writePactFiles) {
      ScalaPactContractWriter.writePactContracts(config.outputPath)(pactWriter)(pactDescription.withHeaderForSsl)
    }

    test(config)
  }

  def startServer(
      strict: Boolean,
      pactDescription: ScalaPactDescriptionFinal
  )(implicit
      httpClient: IScalaPactHttpClient,
      pactReader: IPactReader,
      pactWriter: IPactWriter,
      pactStubber: IPactStubber,
      sslContextMap: SslContextMap
  ): ScalaPactMockServer = {
    val interactionManager: InteractionManager = new InteractionManager

    val protocol   = pactDescription.serverSslContextName.fold("http")(_ => "https")
    val host       = pactDescription.options.host
    val serverPort = pactDescription.options.port
    val outputPath = pactDescription.options.outputPath
    val scalaPactSettings = ScalaPactSettings(
      protocol = Option(protocol),
      host = Option(host),
      port = Option(serverPort),
      localPactFilePath = None,
      strictMode = Option(strict),
      clientTimeout = None,
      outputPath = Option(outputPath),
      publishResultsEnabled = None, // Nothing to publish
      pendingPactSettings = None
    )
    val pacts = List(ScalaPactContractWriter.producePactFromDescription(pactDescription))

    val startStub: ScalaPactSettings => IPactStubber =
      pactStubber.start(interactionManager, 5, pactDescription.serverSslContextName, None)

    val server: IPactStubber = {
      interactionManager.addToInteractionManager(pacts)
      startStub(scalaPactSettings)
    }

    val port: Int = server.port.getOrElse {
      throw new IllegalStateException("Could not obtain the server port")
    }

    val mockConfig = ScalaPactMockConfig(protocol, host, port, outputPath)

    PactLogger.debug("> ScalaPact stub running at: " + mockConfig.baseUrl)

    val mockServer = new ScalaPactMockServer(server, mockConfig)
    waitForServer(mockConfig, pactDescription.serverSslContextName)
    mockServer
  }

  def runConsumerIntegrationTest[A](
      strict: Boolean
  )(pactDescription: ScalaPactDescriptionFinal)(test: ScalaPactMockConfig => A)(implicit
      sslContextMap: SslContextMap,
      pactReader: IPactReader,
      pactWriter: IPactWriter,
      httpClient: IScalaPactHttpClient,
      pactStubber: IPactStubber
  ): A = {
    val server = startServer(strict, pactDescription)
    val result = configuredTestRunner(pactDescription)(server.config)(test)
    server.stop()
    result
  }

  private def waitForServer(
      mockConfig: ScalaPactMockConfig,
      serverSslContextName: Option[String]
  )(implicit httpClient: IScalaPactHttpClient): Unit = {
    @scala.annotation.tailrec
    def rec(attemptsRemaining: Int, intervalMillis: Int): Unit =
      if (isStubReady(mockConfig, serverSslContextName)) {
        PactLogger.debug("Stub server is ready.")
      } else if (attemptsRemaining == 0) {
        throw new Exception("Could not connect to stub at: " + mockConfig.baseUrl)
      } else {
        PactLogger.message(">  ...waiting for stub, attempts remaining: " + attemptsRemaining.toString)
        Thread.sleep(intervalMillis.toLong)
        rec(attemptsRemaining - 1, intervalMillis)
      }

    rec(5, 100)
  }

  private def isStubReady(
      mockConfig: ScalaPactMockConfig,
      sslContextName: Option[String]
  )(implicit httpClient: IScalaPactHttpClient): Boolean =
    httpClient.doRequest(
      SimpleRequest(
        mockConfig.baseUrl,
        "/stub/status",
        HttpMethod.GET,
        Map("X-Pact-Admin" -> "true"),
        None,
        sslContextName = sslContextName
      )
    ) match {
      case Left(_) =>
        false

      case Right(r) =>
        r.is2xx
    }

}

case class ScalaPactMockConfig(protocol: String, host: String, port: Int, outputPath: String) {
  val baseUrl: String = protocol + "://" + host + ":" + port.toString
}

class ScalaPactMockServer(
    underlying: IPactStubber,
    val config: ScalaPactMockConfig
) {
  def stop(): Unit = underlying.shutdown()
}
