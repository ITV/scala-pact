package com.itv.scalapact

import java.io.IOException
import java.net.ServerSocket

import com.github.kristofa.test.http.{Method, MockHttpServer, SimpleHttpResponseProvider}
import com.typesafe.scalalogging.LazyLogging
import com.itv.scalapact.ScalaPactForger._

object ScalaPactMock extends LazyLogging {

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

  def runConsumerIntegrationTest(pactDescription: ScalaPactDescriptionFinal)(test: ScalaPactMockConfig => Unit): Unit = {
    val protocol = "http"
    val host = "localhost"
    val port = findFreePort()

    val responseProvider = new SimpleHttpResponseProvider()

    pactDescription.interactions.foreach { i =>

      //TODO: just tons of nasty gets in here...
      if(i.request.isEmpty || i.response.isEmpty) throw new ScalaPactIncomplete("All pacts must include an expected request and a promised response")
      else {
        val findContentType: Map[String, String] => String = headers => headers.find(h => h._1.toLowerCase == "content-type").map(_._2).getOrElse("text/plain")
        val requestContentType = findContentType(i.request.get.headers)
        val responseContentType = findContentType(i.response.get.headers)

      logger.info(">------------------------------------")
      logger.info("> Adding ScalaPact mock expectation:")
      logger.info("> > ScalaPact mock expecting:\n" + i.request)
      logger.info("> > Derived Content-Type for expected request: " + requestContentType)
      logger.info("> > ScalaPact mock will respond with:\n" + i.response)
      logger.info("> > Found Content-Type to use in response: " + responseContentType)

        i.request.get.method match {
          case GET =>
            responseProvider.expect(Method.GET, i.request.get.path).respondWith(i.response.get.status, responseContentType, i.response.get.body.get)

          case POST =>
            responseProvider.expect(Method.POST, i.request.get.path, requestContentType, i.request.get.body.get).respondWith(i.response.get.status, responseContentType, i.response.get.body.get)

          case PUT =>
            responseProvider.expect(Method.PUT, i.request.get.path, requestContentType, i.request.get.body.get).respondWith(i.response.get.status, responseContentType, i.response.get.body.get)

          case DELETE =>
            responseProvider.expect(Method.DELETE, i.request.get.path, requestContentType, i.request.get.body.get).respondWith(i.response.get.status, responseContentType, i.response.get.body.get)
        }
      }
    }

    val server = new MockHttpServer(port, responseProvider)

    server.start()

    val baseUrl = protocol + "://" + host + ":" + port

    logger.info("> ScalaPact mock running at: " + baseUrl)

    configuredTestRunner(pactDescription)(ScalaPactMockConfig(protocol + "://" + host + ":" + port))(test)

    server.stop()
  }

}

case class ScalaPactMockConfig(baseUrl: String)
class ScalaPactIncomplete(message: String) extends Exception(message)
