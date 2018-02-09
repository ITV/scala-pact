package com.itv.scalapact.shared.http

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.itv.scalapact.shared.{HttpMethod, SimpleRequest}
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

class Http4sClientHelperSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  val wireMockServer = new WireMockServer(wireMockConfig().port(1234))

  override def beforeAll(): Unit = {

    wireMockServer.start()

    WireMock.configureFor("localhost", 1234)

    val response = aResponse().withStatus(200).withBody("Success").withHeader("foo", "bar")

    wireMockServer.stubFor(
      get(urlEqualTo("/test")).willReturn(response)
    )

  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
  }

  describe("Making an HTTP request") {

    it("should be able to make a simple request") {
      val request = SimpleRequest("http://localhost:1234", "/test", HttpMethod.GET,sslContextName = None)

      val response = Http4sClientHelper.doRequest(request, Http4sClientHelper.defaultClient).unsafePerformSync

      response.statusCode shouldEqual 200
      response.body.get shouldEqual "Success"
      response.headers.exists(_ == ("foo" -> "bar")) shouldEqual true
    }

  }

}
