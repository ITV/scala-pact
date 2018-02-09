package com.itv.scalapact.shared

import javax.net.ssl.SSLContext

import org.scalatest.easymock.EasyMockSugar
import org.scalatest.{FunSpec, Matchers}

class SslContextMapSpec extends FunSpec with Matchers {

  describe("SslContextMap.getContext method") {

    val someSslContext = SSLContext.getDefault
    val data = SSLContextData("some", "unused", "data", "", "")
    implicit val sSLContextDataToSslContext = new SSLContextDataToSslContext {
      override def apply(v1: SSLContextData): SSLContext = {
        if (v1 != data) fail()
        someSslContext
      }
    }
    implicit val sslContextMap = new SslContextMap(Map("someSslName" -> data))

    val headersWithoutSslContextHeaderName = Map("a" -> "someA")
    val requestWithNoSslContext = SimpleRequest("http://localhost:1234", "/test", HttpMethod.GET, None)
    val requestWithName = SimpleRequest("http://localhost:1234", "/test", HttpMethod.GET, Some("someSslName"))
    val requestWithWrongName = SimpleRequest("http://localhost:1234", "/test", HttpMethod.GET, Some("invalidSslName"))

    it("should pass a request without ssl context to the block") {
      var called: Int = 0
      SslContextMap(requestWithNoSslContext) { ssl =>
        req =>
          called += 1
          ssl shouldEqual None
          req shouldEqual requestWithNoSslContext
      }
      called shouldEqual 1
    }

    it("should pass a request to the block with SSL if it is present") {
      var called: Int = 0
      SslContextMap(requestWithName) { ssl =>
        req =>
          called += 1
          ssl shouldEqual Some(someSslContext)
          req shouldEqual requestWithName
      }
      called shouldEqual 1
    }

    it("should throw a SslContextNotFoundException if the Ssl Context is not found") {
      var called: Int = 0
      intercept[SslContextNotFoundException](SslContextMap(requestWithWrongName)(ssl => req => fail())).getMessage shouldBe "SslContext [invalidSslName] not found. Legal values are [List(someSslName)]"
    }
  }

}
