package com.itv.scalapact

import java.io.IOException
import java.net.ServerSocket

import com.itv.scalapact.ScalaPactForger._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapact.shared.http.ScalaPactHttpClient
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapactcore.stubber.InteractionManager

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import com.itv.scalapact.shared.PactLogger

object ScalaPactMock {

  private def configuredTestRunner[A](pactDescription: ScalaPactDescriptionFinal)(config: ScalaPactMockConfig)(test: => ScalaPactMockConfig => A)(implicit sslContextMap: SslContextMap): A = {

    if (pactDescription.options.writePactFiles) {
      ScalaPactContractWriter.writePactContracts(config.outputPath)(pactWriter)(pactDescription.withHeaderForSsl)
    }

    test(config)
  }

  // Ported from a Java gist
  private def findFreePort(): Try[Int] = Try {
    val socket: ServerSocket = new ServerSocket(0)
    var port = -1

    try {
      socket.setReuseAddress(false)
      port = socket.getLocalPort

      try {
        socket.close()
      } catch {
        // Ignore IOException on close()
        case _: IOException =>
      }
    } catch {
      case _: IOException =>
    } finally {
      if (socket != null) {
        try {
          socket.close()
        } catch {
          case _: IOException =>
        }
      }
    }

    if (port == -1) throw new IllegalStateException("Could not find a free TCP/IP port to start embedded HTTP Server on")
    else port
  }

  private def findFreePortRetry(attempts: Int): Int = {
    findFreePort() match {
      case Success(port) => port
      case Failure(_) if attempts > 0 => findFreePortRetry(attempts - 1)
      case Failure(e) => throw e
    }
  }

  def runConsumerIntegrationTest[A](strict: Boolean)(pactDescription: ScalaPactDescriptionFinal)(test: ScalaPactMockConfig => A)(implicit sslContextMap: SslContextMap): A = {

    val interactionManager: InteractionManager = new InteractionManager

    val protocol = pactDescription.optContextNameAndClientAuth.fold("http")(_ => "https")
    val mockConfig = ScalaPactMockConfig(protocol, "localhost", findFreePortRetry(3), pactDescription.options.outputPath)
    val configAndPacts: ConfigAndPacts = ConfigAndPacts(
      scalaPactSettings = ScalaPactSettings(
        host = Option(mockConfig.host),
        protocol = Option(mockConfig.protocol),
        port = Option(mockConfig.port),
        localPactFilePath = None,
        strictMode = Option(strict),
        clientTimeout = Option(Duration(2, SECONDS)), // Should never ever take this long. Used to make an http request against the local stub.
        outputPath = Option(mockConfig.outputPath)
      ),
      pacts = List(ScalaPactContractWriter.producePactFromDescription(pactDescription))
    )

    val connectionPoolSize: Int = 5

    val server: IPactServer = (interactionManager.addToInteractionManager andThen runServer(interactionManager, connectionPoolSize, pactDescription.optContextNameAndClientAuth, configAndPacts.scalaPactSettings.givePort)) (configAndPacts)

    PactLogger.message("> ScalaPact stub running at: " + mockConfig.baseUrl)

    waitForServerThenTest(server, mockConfig, test, pactDescription)
  }

  private def waitForServerThenTest[A](server: IPactServer, mockConfig: ScalaPactMockConfig, test: ScalaPactMockConfig => A, pactDescription: ScalaPactDescriptionFinal)(implicit sslContextMap: SslContextMap): A = {
    def rec(attemptsRemaining: Int, intervalMillis: Int): A = {
      if (isStubReady(mockConfig, pactDescription.optContextNameAndClientAuth.map(_.name))) {
        val result = configuredTestRunner(pactDescription)(mockConfig)(test)

        stopServer(server)

        result
      } else if (attemptsRemaining == 0) {
        throw new Exception("Could not connect to stub are: " + mockConfig.baseUrl)
      } else {
        PactLogger.message(">  ...waiting for stub, attempts remaining: " + attemptsRemaining.toString)
        Thread.sleep(intervalMillis.toLong)
        rec(attemptsRemaining - 1, intervalMillis)
      }
    }

    rec(5, 100)
  }

  private def isStubReady(mockConfig: ScalaPactMockConfig, sslContextName: Option[String])(implicit sslContextMap: SslContextMap): Boolean = {
    ScalaPactHttpClient.doRequestSync(SimpleRequest(mockConfig.baseUrl, "/stub/status", HttpMethod.GET, Map("X-Pact-Admin" -> "true"), None, sslContextName = sslContextName)) match {
      case Left(_) =>
        false

      case Right(r) =>
        r.is2xx
    }
  }

}

case class ScalaPactMockConfig(protocol: String, host: String, port: Int, outputPath: String) {
  val baseUrl: String = protocol + "://" + host + ":" + port.toString
}
