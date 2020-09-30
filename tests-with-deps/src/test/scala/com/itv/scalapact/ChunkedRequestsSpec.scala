package com.itv.scalapact

import com.itv.scalapact.ScalaPactForger._
import org.scalatest.{FunSpec, Matchers}
import fr.hmil.roshttp.{BackendConfig, HttpRequest}
import monix.execution.Scheduler.Implicits.global
import fr.hmil.roshttp.body.{BulkBodyPart, PlainTextBody}
import java.nio.ByteBuffer

import fr.hmil.roshttp.response.SimpleHttpResponse

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Strict and non-strict pacts cannot be mixed.
 **/
class ChunkedRequestsSpec extends FunSpec with Matchers {

  import com.itv.scalapact.json._
  import com.itv.scalapact.http._

  describe("Supporting clients that send chunked request bodies") {

    it("Should be able to accept a chunked request") {

      val endPoint = "/chunked"

      forgePact
        .between("My Chunked Consumer")
        .and("Their Chunked Provider Service")
        .addInteraction(
          interaction
            .description("a chunked request")
            .uponReceiving(
              method = POST,
              path = endPoint,
              query = None,
              headers = Map.empty,
              body = xml,
            )
            .willRespondWith(
              status = 200,
              headers = Map.empty,
              body = "Success"
            )
        )
        .runConsumerTest { mockConfig =>
          val request =
            HttpRequest(mockConfig.baseUrl + endPoint)
              .withBackendConfig(
                BackendConfig(
                  allowChunkedRequestBody = true
                )
              )

          val res: SimpleHttpResponse = Await.result(request.post(PlainTextBody(xml)), 5.second)

          res.statusCode shouldEqual 200
          res.body shouldEqual "Success"

        }

    }

    it("should be able to accept a chunked request with realistic XML") {

      val endPoint = "/chunked2"

      forgePact
        .between("My Chunked Consumer")
        .and("Their Chunked Provider Service")
        .addInteraction(
          interaction
            .description("another chunked request")
            .uponReceiving(
              method = POST,
              path = endPoint,
              query = None,
              headers = Map("SOAPAction"   -> "process",
                            "Accept"       -> "text/xml",
                            "Content-Type" -> "text/xml; charset=utf-8",
                            "User-Agent"   -> "scala-pact-test"),
              body = xml,
            )
            .willRespondWith(
              status = 200,
              headers = Map.empty,
              body = "Success",
            )
        )
        .runConsumerTest { mockConfig =>
          val request =
            HttpRequest()
              .withBackendConfig(
                BackendConfig(
                  allowChunkedRequestBody = true
                )
              )
              .withURL(mockConfig.baseUrl)
              .withPath(endPoint)
              .withHeaders("SOAPAction"   -> "process",
                           "Accept"       -> "text/xml",
                           "Content-Type" -> "text/xml",
                           "User-Agent"   -> "scala-pact-test")
              .withMethod(fr.hmil.roshttp.Method.POST)
              .withBody(XmlBody(xml))

          val res: SimpleHttpResponse = Await.result(request.send(), 1.second)

          res.statusCode shouldEqual 200
          res.body shouldEqual "Success"

        }

    }

  }

  def xml: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<bookstore>
      |  <book category="cooking">
      |    <title lang="en">Everyday Italian</title>
      |    <author>Giada De Laurentiis</author>
      |    <year>2005</year>
      |    <price>30.00</price>
      |  </book>
      |  <book category="children">
      |    <title lang="en">Harry Potter</title>
      |    <author>J K. Rowling</author>
      |    <year>2005</year>
      |    <price>29.99</price>
      |  </book>
      |  <book category="web">
      |    <title lang="en">Learning XML</title>
      |    <author>Erik T. Ray</author>
      |    <year>2003</year>
      |    <price>39.95</price>
      |  </book>
      |  <book category="cooking">
      |    <title lang="en">Everyday Italian</title>
      |    <author>Giada De Laurentiis</author>
      |    <year>2005</year>
      |    <price>30.00</price>
      |  </book>
      |  <book category="children">
      |    <title lang="en">Harry Potter</title>
      |    <author>J K. Rowling</author>
      |    <year>2005</year>
      |    <price>29.99</price>
      |  </book>
      |  <book category="web">
      |    <title lang="en">Learning XML</title>
      |    <author>Erik T. Ray</author>
      |    <year>2003</year>
      |    <price>39.95</price>
      |  </book>
      |  <book category="cooking">
      |    <title lang="en">Everyday Italian</title>
      |    <author>Giada De Laurentiis</author>
      |    <year>2005</year>
      |    <price>30.00</price>
      |  </book>
      |  <book category="children">
      |    <title lang="en">Harry Potter</title>
      |    <author>J K. Rowling</author>
      |    <year>2005</year>
      |    <price>29.99</price>
      |  </book>
      |  <book category="web">
      |    <title lang="en">Learning XML</title>
      |    <author>Erik T. Ray</author>
      |    <year>2003</year>
      |    <price>39.95</price>
      |  </book>
      |  <book category="cooking">
      |    <title lang="en">Everyday Italian</title>
      |    <author>Giada De Laurentiis</author>
      |    <year>2005</year>
      |    <price>30.00</price>
      |  </book>
      |  <book category="children">
      |    <title lang="en">Harry Potter</title>
      |    <author>J K. Rowling</author>
      |    <year>2005</year>
      |    <price>29.99</price>
      |  </book>
      |  <book category="web">
      |    <title lang="en">Learning XML</title>
      |    <author>Erik T. Ray</author>
      |    <year>2003</year>
      |    <price>39.95</price>
      |  </book>
      |  <book category="cooking">
      |    <title lang="en">Everyday Italian</title>
      |    <author>Giada De Laurentiis</author>
      |    <year>2005</year>
      |    <price>30.00</price>
      |  </book>
      |  <book category="children">
      |    <title lang="en">Harry Potter</title>
      |    <author>J K. Rowling</author>
      |    <year>2005</year>
      |    <price>29.99</price>
      |  </book>
      |  <book category="web">
      |    <title lang="en">Learning XML</title>
      |    <author>Erik T. Ray</author>
      |    <year>2003</year>
      |    <price>39.95</price>
      |  </book>
      |</bookstore>""".stripMargin

}

class XmlBody private (text: String, charset: String) extends BulkBodyPart {

  override def contentType: String = "text/xml; charset=" + charset

  override def contentData: ByteBuffer = ByteBuffer.wrap(text.getBytes())
}

object XmlBody {
  def apply(text: String, charset: String = "utf-8"): XmlBody = new XmlBody(text, charset)
}
