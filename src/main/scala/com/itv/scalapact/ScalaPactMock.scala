package com.itv.scalapact

import java.io.IOException
import java.net.ServerSocket

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.itv.scalapact.ScalaPactForger._
import com.typesafe.scalalogging.LazyLogging

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

    val wireMockServer = new WireMockServer(wireMockConfig().port(port))

    wireMockServer.start()

    WireMock.configureFor(host, port)

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
        case GET =>
          injectStub(
            mappingBuilder = get(urlEqualTo(i.request.path)),
            request = i.request,
            response = i.response
          )

        case POST =>
          injectStub(
            mappingBuilder = post(urlEqualTo(i.request.path)),
            request = i.request,
            response = i.response
          )

        case PUT =>
          injectStub(
            mappingBuilder = put(urlEqualTo(i.request.path)),
            request = i.request,
            response = i.response
          )

        case DELETE =>
          injectStub(
            mappingBuilder = delete(urlEqualTo(i.request.path)),
            request = i.request,
            response = i.response
          )

      }
    }

    val baseUrl = protocol + "://" + host + ":" + port

    logger.info("> ScalaPact mock running at: " + baseUrl)

    configuredTestRunner(pactDescription)(ScalaPactMockConfig(protocol + "://" + host + ":" + port))(test)

    wireMockServer.stop()
  }

  def injectStub(mappingBuilder: MappingBuilder, request: ScalaPactRequest, response: ScalaPactResponse): Unit = {

    request.headers.foreach { h =>
      mappingBuilder.withHeader(h._1, equalTo(h._2))
    }

    request.body.map { b =>
      mappingBuilder.withRequestBody(equalTo(b))
    }

    stubFor(
      mappingBuilder.willReturn {
        val mockResponse = aResponse()
          .withStatus(response.status)

        response.body.map { b =>
          mockResponse.withBody(b)
        }

        response.headers.foreach(h => mockResponse.withHeader(h._1, h._2))

        mockResponse
      }
    )
  }

}

case class ScalaPactMockConfig(baseUrl: String)
