package com.itv.scalapact

import java.io.IOException
import java.net.ServerSocket

import com.itv.scalapact.ScalaPactForger._
import com.itv.scalapactcore.common.{Arguments, ConfigAndPacts, ScalaPactHttp}
import com.itv.scalapactcore.stubber.InteractionManager
import com.itv.scalapactcore.stubber.PactStubService._
import org.http4s.server.Server

object ScalaPactMock {

  private def configuredTestRunner[A](pactDescription: ScalaPactDescriptionFinal)(config: ScalaPactMockConfig)(test: => ScalaPactMockConfig => A): A = {

    if(pactDescription.options.writePactFiles) {
      ScalaPactContractWriter.writePactContracts(pactDescription)
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
      arguments = Arguments(
        host = Option(mockConfig.host),
        protocol = Option(mockConfig.protocol),
        port = Option(mockConfig.port),
        localPactPath = None,
        strictMode = Option(strict),
        clientTimeout = 2 // Should never ever take this long. Used to make an http request against the local stub.
      ),
      pacts = List(ScalaPactContractWriter.producePactFromDescription(pactDescription))
    )

    val connectionPoolSize: Int = 5

    val server = (interactionManager.addToInteractionManager andThen runServer(interactionManager)(connectionPoolSize))(configAndPacts)

    println("> ScalaPact stub running at: " + mockConfig.baseUrl)

    waitForServerThenTest(server, mockConfig, test, pactDescription)
  }

  private def waitForServerThenTest[A](server: Server, mockConfig: ScalaPactMockConfig, test: ScalaPactMockConfig => A, pactDescription: ScalaPactDescriptionFinal): A = {
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

  private def isStubReady(mockConfig: ScalaPactMockConfig): Boolean =
    ScalaPactHttp.doRequest(ScalaPactHttp.GET, mockConfig.baseUrl, "/stub/status", Map("X-Pact-Admin" -> "true"), None).unsafePerformSync.is2xx

}

case class ScalaPactMockConfig(protocol: String, host: String, port: Int) {
  val baseUrl: String = protocol + "://" + host + ":" + port
}
