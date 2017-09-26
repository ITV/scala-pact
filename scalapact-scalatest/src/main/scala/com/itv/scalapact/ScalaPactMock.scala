package com.itv.scalapact

import java.io.IOException
import java.net.ServerSocket

import com.itv.scalapact.ScalaPactForger._
import com.itv.scalapact.shared._
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapact.shared.http.ScalaPactHttpClient

import scala.concurrent.duration._

object ScalaPactMock {

  private def configuredTestRunner[A](pactDescription: ScalaPactDescriptionFinal)(config: ScalaPactMockConfig)(test: => ScalaPactMockConfig => A): A = {

    if(pactDescription.options.writePactFiles) {
      ScalaPactContractWriter.writePactContracts(pactWriter)(pactDescription)
    }

    test(config)
  }

  // Ported from a Java gist
  private def findFreePort(): Int = {
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
    } catch{
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

    if(port == -1) throw new IllegalStateException("Could not find a free TCP/IP port to start embedded HTTP Server on")
    else port
  }

  def runConsumerIntegrationTest[A](strict: Boolean)(pactDescription: ScalaPactDescriptionFinal)(test: ScalaPactMockConfig => A): A = {

    val interactionManager: InteractionManager = new InteractionManager

    val mockConfig = ScalaPactMockConfig("http", "localhost", findFreePort())

    val configAndPacts: ConfigAndPacts = ConfigAndPacts(
      scalaPactSettings = ScalaPactSettings(
        host = Option(mockConfig.host),
        protocol = Option(mockConfig.protocol),
        port = Option(mockConfig.port),
        localPactFilePath = None,
        strictMode = Option(strict),
        clientTimeout = Option(Duration(2, SECONDS)) // Should never ever take this long. Used to make an http request against the local stub.
      ),
      pacts = List(ScalaPactContractWriter.producePactFromDescription(pactDescription))
    )

    val connectionPoolSize: Int = 5

    val server: IPactServer = (interactionManager.addToInteractionManager andThen runServer(interactionManager, connectionPoolSize))(configAndPacts)

    println("> ScalaPact stub running at: " + mockConfig.baseUrl)

    waitForServerThenTest(server, mockConfig, test, pactDescription)
  }

  private def waitForServerThenTest[A](server: IPactServer, mockConfig: ScalaPactMockConfig, test: ScalaPactMockConfig => A, pactDescription: ScalaPactDescriptionFinal): A = {
    def rec(attemptsRemaining: Int, intervalMillis: Int): A = {
      if(isStubReady(mockConfig)) {
        val result = configuredTestRunner(pactDescription)(mockConfig)(test)

        stopServer(server)

        result
      } else if(attemptsRemaining == 0) {
        throw new Exception("Could not connect to stub are: " + mockConfig.baseUrl)
      } else {
        println(">  ...waiting for stub, attempts remaining: " + attemptsRemaining)
        Thread.sleep(intervalMillis)
        rec(attemptsRemaining - 1, intervalMillis)
      }
    }

    rec(5, 100)
  }

  private def isStubReady(mockConfig: ScalaPactMockConfig): Boolean = {
    ScalaPactHttpClient.doRequestSync(SimpleRequest(mockConfig.baseUrl, "/stub/status", HttpMethod.GET, Map("X-Pact-Admin" -> "true"), None)) match {
      case Left(_) =>
        false

      case Right(r) =>
        r.is2xx
    }
  }

}

case class ScalaPactMockConfig(protocol: String, host: String, port: Int) {
  val baseUrl: String = protocol + "://" + host + ":" + port
}
