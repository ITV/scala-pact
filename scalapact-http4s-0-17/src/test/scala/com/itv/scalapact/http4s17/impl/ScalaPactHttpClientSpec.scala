package com.itv.scalapact.http4s17.impl

import com.itv.scalapact.shared._
import fs2.Task
import org.http4s.client.Client
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._

class ScalaPactHttpClientSpec extends FunSpec with Matchers with MockFactory {

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

  describe("Making an interaction request") {

    it("should be able to make and interaction request and get an interaction response") {
      val fakeCaller: (SimpleRequest, Client) => Task[SimpleResponse] = (_, _) => Task.now(SimpleResponse(200))
      val result = new ScalaPactHttpClient(fakeCaller)
        .doInteractionRequestTask(fakeCaller, "", requestDetails, 1.second, sslContextName = None)
        .unsafeRun()
      result shouldEqual responseDetails
    }

    // Does this test anything other than that the arguments are passed through correctly?
//    it("should pass the SSL context to the client builder") {
//
//      val sslContext             = mock[SSLContext]
//      implicit val sslContextMap = new SslContextMap(Map("someSSLName" -> sslContext))
//      val client                 = "the client"
//      val fakeCaller: (SimpleRequest, String) => Task[SimpleResponse] = (sr, actualClient) => {
//        sr shouldBe SimpleRequest("", "/foo", HttpMethod.GET, Some("someSSLName"))
//        actualClient shouldBe client
//        Task.now(SimpleResponse(200))
//      }
//
//      val result = new ScalaPactHttpClient[String] {
//        override def buildClient = new BuildClient[String] {
//          override def apply(maxTotalConntections: Int,
//                             clientTimeout: Duration,
//                             actualSslContext: Option[SSLContext]): String = {
//            actualSslContext shouldBe Some(sslContext)
//            clientTimeout shouldBe 1.second
//            maxTotalConntections shouldBe 1
//            client
//          }
//        }
//
//        override def doRequest = ???
//      }.doInteractionRequestTask(fakeCaller, "", requestDetails, 1.second, sslContextName = Some("someSSLName"))
//        .unsafeRun()
//
//      result shouldEqual responseDetails
//
//    }

  }

}
