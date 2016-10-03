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

    val mockConfig = ScalaPactMockConfig("http", "localhost", findFreePort())

    val wireMockServer = new WireMockServer(wireMockConfig().port(mockConfig.port))

    wireMockServer.start()

    WireMock.configureFor(mockConfig.host, mockConfig.port)

    pactDescription.interactions.foreach { i =>

      val findContentType: Map[String, String] => String = headers => headers.find(h => h._1.toLowerCase == "content-type").map(_._2).getOrElse("text/plain")
      val requestContentType = findContentType(i.request.headers)
      val responseContentType = findContentType(i.response.headers)

      val constructedPath = i.request.path.split('?').toList ++ List(i.request.query.getOrElse("")) match {
        case Nil => "/"
        case x :: xs => List(x, xs.filter(!_.isEmpty).mkString("&")).filterNot(_.isEmpty).mkString("?")
      }

      logger.info(">------------------------------------")
      logger.info("> Adding ScalaPact mock expectation:")
      logger.info("> > ScalaPact mock expecting:\n" + i.request)
      logger.info("> > Derived Content-Type for expected request: " + requestContentType)
      logger.info("> > ScalaPact mock will respond with:\n" + i.response)
      logger.info("> > Found Content-Type to use in response: " + responseContentType)
      logger.info("> > Constructed path: " + constructedPath)


      i.request.method match {
        case GET =>
          injectStub(
            wireMockServer = wireMockServer,
            mappingBuilder = get(urlEqualTo(constructedPath)),
            request = i.request,
            response = i.response
          )

        case OPTIONS =>
          injectStub(
            wireMockServer = wireMockServer,
            mappingBuilder = WireMock.options(urlEqualTo(constructedPath)),
            request = i.request,
            response = i.response
          )

        case POST =>
          injectStub(
            wireMockServer = wireMockServer,
            mappingBuilder = post(urlEqualTo(constructedPath)),
            request = i.request,
            response = i.response
          )

        case PUT =>
          injectStub(
            wireMockServer = wireMockServer,
            mappingBuilder = put(urlEqualTo(constructedPath)),
            request = i.request,
            response = i.response
          )

        case DELETE =>
          injectStub(
            wireMockServer = wireMockServer,
            mappingBuilder = delete(urlEqualTo(constructedPath)),
            request = i.request,
            response = i.response
          )

        case OPTIONS =>
          injectStub(
            wireMockServer = wireMockServer,
            mappingBuilder = WireMock.options(urlEqualTo(constructedPath)),
            request = i.request,
            response = i.response
          )

      }
    }

    logger.info("> ScalaPact mock running at: " + mockConfig.baseUrl)

    configuredTestRunner(pactDescription)(mockConfig)(test)

    wireMockServer.stop()
  }

  private val extractMatchRuleKey: String => String => Option[String] = prefix => key =>
    if(key.startsWith(prefix)) Option(key.substring(prefix.length)) else None

  private val headerKey: String => Option[String] = key => extractMatchRuleKey("$.headers.")(key)
  private val bodyKey: String => Option[String] = key => extractMatchRuleKey("$.headers.")(key)

  def injectStub(wireMockServer: WireMockServer, mappingBuilder: MappingBuilder, request: ScalaPactRequest, response: ScalaPactResponse): Unit = {

    request.headers.foreach { h =>
      request.matchingRules.getOrElse(Nil).find(p => headerKey(p.key).contains(h._1)) match {
          case r @ Some(ScalaPactMatchingRuleRegex(key, regex))  =>
            mappingBuilder.withHeader(h._1, matching(regex))

          case t @ Some(ScalaPactMatchingRuleType(key))  =>
            // Type matching rules are for body matchers, headers are always strings.
            mappingBuilder.withHeader(h._1, equalTo(h._2))

          case None =>
            mappingBuilder.withHeader(h._1, equalTo(h._2))
      }
    }

    request.body.map { b =>
      mappingBuilder.withRequestBody(equalTo(b))
    }

    wireMockServer.stubFor(
      mappingBuilder.willReturn {
        val mockResponse = aResponse()
          .withStatus(response.status)

        response.body.map { b =>
          mockResponse.withBody(b)

          if (!response.headers.contains("Content-Length"))
            mockResponse.withHeader("Content-Length", b.length.toString)
        }

        response.headers.foreach(h => mockResponse.withHeader(h._1, h._2))

        mockResponse
      }
    )
  }

}

case class ScalaPactMockConfig(protocol: String, host: String, port: Int) {
  val baseUrl: String = protocol + "://" + host + ":" + port
}
