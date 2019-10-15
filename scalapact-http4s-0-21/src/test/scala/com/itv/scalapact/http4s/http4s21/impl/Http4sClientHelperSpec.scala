package com.itv.scalapact.http4s.http4s21.impl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.itv.scalapact.http4s21.impl.Http4sClientHelper
import com.itv.scalapact.shared.{HttpMethod, SimpleRequest}
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

class Http4sClientHelperSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  private val wireMockServer = new WireMockServer(wireMockConfig().port(0))

  private def port(): Int = wireMockServer.port

  override def beforeAll(): Unit = {

    wireMockServer.start()

    WireMock.configureFor("localhost", port())

    val response = aResponse().withStatus(200).withBody("Success").withHeader("foo", "bar")

    wireMockServer.stubFor(
      get(urlEqualTo("/test")).willReturn(response)
    )

  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  describe("Making an HTTP request") {

    it("should be able to make a simple request") {
      val request  = SimpleRequest(s"http://localhost:${port()}", "/test", HttpMethod.GET, None)
      val response = Http4sClientHelper.doRequest(request, Http4sClientHelper.defaultClient).unsafeRunSync()

      response.statusCode shouldEqual 200
      response.body.get shouldEqual "Success"
      response.headers.exists(_ == ("foo" -> "bar")) shouldEqual true
    }

  }

}
