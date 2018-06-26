package com.itv.scalapact

import com.itv.scalapact.ScalaPactForger._
import com.itv.scalapact.shared.{PactLogger, _}
import com.itv.scalapact.shared.typeclasses.{IPactReader, IPactStubber, IPactWriter, IScalaPactHttpClient}
import com.itv.scalapactcore.common.stubber.InteractionManager

import scala.concurrent.duration._

object ScalaPactMock {

  private def configuredTestRunner[A](
      pactDescription: ScalaPactDescriptionFinal
  )(config: ScalaPactMockConfig)(test: => ScalaPactMockConfig => A)(implicit pactWriter: IPactWriter): A = {

    if (pactDescription.options.writePactFiles) {
      ScalaPactContractWriter.writePactContracts(config.outputPath)(pactWriter)(pactDescription.withHeaderForSsl)
    }

    test(config)
  }

  def runConsumerIntegrationTest[F[_], A](
      strict: Boolean
  )(pactDescription: ScalaPactDescriptionFinal)(test: ScalaPactMockConfig => A)(implicit sslContextMap: SslContextMap,
                                                                                pactReader: IPactReader,
                                                                                pactWriter: IPactWriter,
                                                                                httpClient: IScalaPactHttpClient[F],
                                                                                pactStubber: IPactStubber): A = {

    val interactionManager: InteractionManager = new InteractionManager

    val protocol = pactDescription.serverSslContextName.fold("http")(_ => "https")
    val host = "localhost"
    val outputPath = pactDescription.options.outputPath

    val configAndPacts: ConfigAndPacts = ConfigAndPacts(
      scalaPactSettings = ScalaPactSettings(
        protocol = Option(protocol),
        host = Option(host),
        port = Option(0), // `0` means "use any available port".
        localPactFilePath = None,
        strictMode = Option(strict),
        clientTimeout = Option(Duration(2, SECONDS)), // Should never ever take this long. Used to make an http request against the local stub.
        outputPath = Option(outputPath)
      ),
      pacts = List(ScalaPactContractWriter.producePactFromDescription(pactDescription))
    )

    val startStub: ScalaPactSettings => IPactStubber =
      pactStubber.start(interactionManager, 5, pactDescription.serverSslContextName, None)

    val server: IPactStubber = (interactionManager.addToInteractionManager andThen startStub)(configAndPacts)

    val port: Int = server.port.getOrElse {
      throw new IllegalStateException("Could not obtain the server port")
    }

    val mockConfig = ScalaPactMockConfig(protocol, host, port, outputPath)

    PactLogger.debug("> ScalaPact stub running at: " + mockConfig.baseUrl)

    waitForServerThenTest(server, mockConfig, test, pactDescription)
  }

  private def waitForServerThenTest[F[_], A](
      server: IPactStubber,
      mockConfig: ScalaPactMockConfig,
      test: ScalaPactMockConfig => A,
      pactDescription: ScalaPactDescriptionFinal
  )(implicit sslContextMap: SslContextMap, pactWriter: IPactWriter, httpClient: IScalaPactHttpClient[F]): A = {
    def rec(attemptsRemaining: Int, intervalMillis: Int): A =
      if (isStubReady(mockConfig, pactDescription.serverSslContextName)) {
        val result = configuredTestRunner(pactDescription)(mockConfig)(test)

        server.shutdown()

        result
      } else if (attemptsRemaining == 0) {
        throw new Exception("Could not connect to stub at: " + mockConfig.baseUrl)
      } else {
        PactLogger.message(">  ...waiting for stub, attempts remaining: " + attemptsRemaining.toString)
        Thread.sleep(intervalMillis.toLong)
        rec(attemptsRemaining - 1, intervalMillis)
      }

    rec(5, 100)
  }

  private def isStubReady[F[_]](
      mockConfig: ScalaPactMockConfig,
      sslContextName: Option[String]
  )(implicit sslContextMap: SslContextMap, httpClient: IScalaPactHttpClient[F]): Boolean =
    httpClient.doRequestSync(
      SimpleRequest(mockConfig.baseUrl,
                    "/stub/status",
                    HttpMethod.GET,
                    Map("X-Pact-Admin" -> "true"),
                    None,
                    sslContextName = sslContextName)
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
