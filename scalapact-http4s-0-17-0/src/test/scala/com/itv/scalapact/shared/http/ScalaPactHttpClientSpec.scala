package com.itv.scalapact.shared.http

import javax.net.ssl.SSLContext

import com.itv.scalapact.shared._
import fs2.Task
import org.http4s.client.Client
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._

class ScalaPactHttpClientSpec extends FunSpec with Matchers with MockitoSugar {

  val requestDetails = InteractionRequest(
    method = Some("GET"),
    headers = None,
    query = None,
    path = Some("/foo"),
    body = None,
    matchingRules = None
  )

  val responseDetails = InteractionResponse(
    status = Some(200),
    headers = None,
    body = None,
    matchingRules = None
  )


  val someSslContext = mock[SSLContext]
  val data = SSLContextData("some", "unused", "data", "", "")
  implicit val sSLContextDataToSslContext = new SSLContextDataToSslContext {
    override def apply(v1: SSLContextData): SSLContext = {
      if (v1 != data) fail()
      someSslContext
    }
  }
  implicit val sslContextMap = new SslContextMap(Map("someSslName" -> data))

  describe("Making an interaction request") {

    it("should be able to make and interaction request and get an interaction response") {
      val fakeCaller: (SimpleRequest, Client) => Task[SimpleResponse] = (_, _) => Task.now(SimpleResponse(200))
      val result = ScalaPactHttpClient.doInteractionRequestTask(fakeCaller, "", requestDetails, 1.second, sslContextName = None).unsafeRun()
      result shouldEqual responseDetails
    }

    it("should pass the SSL context to the client builder") {
      val client = "the client"
      val fakeCaller: (SimpleRequest, String) => Task[SimpleResponse] = (sr, actualClient) => {
        sr shouldBe SimpleRequest("", "/foo", HttpMethod.GET, Some("someSslName"))
        actualClient shouldBe client
        Task.now(SimpleResponse(200))
      }

      val result = new ScalaPactHttpClient[String] {
        override def buildClient = new BuildClient[String] {
          override def apply(maxTotalConntections: Int, clientTimeout: Duration, actualSslContext: Option[SSLContext]): String = {
            actualSslContext shouldBe Some(someSslContext)
            clientTimeout shouldBe 1.second
            maxTotalConntections shouldBe 1
            client
          }
        }


        override def doRequest = ???
      }.doInteractionRequestTask(fakeCaller, "", requestDetails, 1.second, sslContextName = Some("someSslName")).unsafeRun()

      result shouldEqual responseDetails

    }

  }

}
