package com.itv.scalapact

import java.io.IOException
import java.net.ServerSocket

import com.github.kristofa.test.http.{Method, MockHttpServer, SimpleHttpResponseProvider}
import com.typesafe.scalalogging.LazyLogging

object ScalaPactMock extends LazyLogging {

  private def configuredTestRunner(pactDescription: DescribesPactBetween)(config: ScalaPactMockConfig)(test: => ScalaPactMockConfig => Unit) = {

    if(pactDescription.options.isDefined && pactDescription.options.get.writePactFiles) {
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

  def runConsumerIntegrationTest(pactDescription: DescribesPactBetween)(test: ScalaPactMockConfig => Unit): Unit = {
    val protocol = "http"
    val host = "localhost"
    val port = findFreePort()

    val responseProvider = new SimpleHttpResponseProvider()

    pactDescription.interactions.foreach { i =>

      val findContentType: Map[String, String] => String = headers => headers.find(h => h._1.toLowerCase == "content-type").map(_._2).getOrElse("text/plain")
      val requestContentType = findContentType(i.request.headers)
      val responseContentType = findContentType(i.response.headers)

      logger.info(">------------------------------------")
      logger.info("> Adding ScalaPact mock expectation:")
      logger.info("> > ScalaPact mock expecting:\n" + i.request)
      logger.info("> > Derived Content-Type for expected request: " + requestContentType)
      logger.info("> > ScalaPact mock will respond with:\n" + i.response)
      logger.info("> > Found Content-Type to use in response: " + responseContentType)

      i.request.method match {
        case ScalaPactMethods.GET =>
          responseProvider.expect(Method.GET, i.request.path).respondWith(i.response.status, responseContentType, i.response.body)

        case ScalaPactMethods.POST =>
          responseProvider.expect(Method.POST, i.request.path, requestContentType, i.request.body).respondWith(i.response.status, responseContentType, i.response.body)

        case ScalaPactMethods.PUT =>
          responseProvider.expect(Method.PUT, i.request.path, requestContentType, i.request.body).respondWith(i.response.status, responseContentType, i.response.body)

        case ScalaPactMethods.DELETE =>
          responseProvider.expect(Method.DELETE, i.request.path, requestContentType, i.request.body).respondWith(i.response.status, responseContentType, i.response.body)
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
