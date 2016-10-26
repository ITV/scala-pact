package com.itv.scalapact

import java.io.IOException
import java.net.ServerSocket

import com.itv.scalapact.ScalaPactForger._
import com.itv.scalapactcore.common.{Arguments, ConfigAndPacts}
import com.itv.scalapactcore.stubber.InteractionManager._
import com.itv.scalapactcore.stubber.PactStubService._

object ScalaPactMock {

  private def configuredTestRunner(pactDescription: ScalaPactDescriptionFinal)(config: ScalaPactMockConfig)(test: => ScalaPactMockConfig => Unit) = {

    if(pactDescription.options.writePactFiles) {
      ScalaPactContractWriter.writePactContracts(pactDescription)
    }

    test(config)

    ()
  }

  // Ported from a Java gist
  private def findFreePort(): Int = {
    val socket: ServerSocket = new ServerSocket(0)
    var port = -1

    try {
      socket.setReuseAddress(true)
      port = socket.getLocalPort

      try {
        socket.close()
      } catch {
        // Ignore IOException on close()
        case e: IOException =>
      }
    } catch{
      case e: IOException =>
    } finally {
      if (socket != null) {
        try {
          socket.close()
        } catch {
          case e: IOException =>
        }
      }
    }

    if(port == -1) throw new IllegalStateException("Could not find a free TCP/IP port to start embedded Jetty HTTP Server on")
    else port
  }

  def runConsumerIntegrationTest(strict: Boolean)(pactDescription: ScalaPactDescriptionFinal)(test: ScalaPactMockConfig => Unit): Unit = {

    val mockConfig = ScalaPactMockConfig("http", "localhost", findFreePort())

    val configAndPacts: ConfigAndPacts = ConfigAndPacts(
      arguments = Arguments(
        host = Option(mockConfig.host),
        protocol = Option(mockConfig.protocol),
        port = Option(mockConfig.port),
        localPactPath = None,
        strictMode = Option(strict) // TODO: Should be able to decide which mode to use.
      ),
      pacts = List(ScalaPactContractWriter.producePactFromDescription(pactDescription))
    )

    val server = (addToInteractionManager andThen runServer)(configAndPacts)

    // This if naff, but sometimes isn't quite ready when we run the first test for some reason.
    // TODO: Fix this properly
    Thread.sleep(100)

    println("> ScalaPact mock running at: " + mockConfig.baseUrl)

    configuredTestRunner(pactDescription)(mockConfig)(test)

    stopServer(server)
  }

}

case class ScalaPactMockConfig(protocol: String, host: String, port: Int) {
  val baseUrl: String = protocol + "://" + host + ":" + port
}
