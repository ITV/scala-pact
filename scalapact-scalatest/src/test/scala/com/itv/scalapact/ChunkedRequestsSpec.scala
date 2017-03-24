package com.itv.scalapact

import com.itv.scalapact.ScalaPactForger._
import org.scalatest.{FunSpec, Matchers}
import fr.hmil.roshttp.{BackendConfig, HttpRequest}
import monix.execution.Scheduler.Implicits.global
import fr.hmil.roshttp.body.PlainTextBody

import scala.util.{Failure, Success}
import fr.hmil.roshttp.response.SimpleHttpResponse

import scala.concurrent.duration._

import scala.concurrent.Await

/**
 * Strict and non-strict pacts cannot be mixed.
 **/
class ChunkedRequestsSpec extends FunSpec with Matchers {

  describe("Supporting clients that send chunked request bodies") {

    it("Should be able to accept a chunked request") {

      val endPoint = "/chunked"

      val xml: String =
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
              matchingRules = None
            )
            .willRespondWith(
              status = 200,
              headers = Map.empty,
              body = "Success",
              matchingRules = None
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

          val res = Await.result(request.post(PlainTextBody(xml)), 1.second)

          res.get.statusCode shouldEqual 200
          res.get.body shouldEqual "Success"

        }

    }

  }

}
