package com.itv.scalapact.plugin.publish

import com.itv.scalapact.plugin.{HttpService, PactFormatter, PublishAddressGenerator}
import com.itv.scalapactcore.Pact
import org.mockito.ArgumentCaptor
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

import scalaj.http.{HttpRequest, HttpResponse}


class PublisherSpec extends FlatSpec with Matchers with MockitoSugar {

  import org.mockito.Mockito._

  val pact = mock[Pact]
  val details = PublishDetails("someAddress", "someProvider", "someConsumer", "someVersion", pact)

  "address generator" should "make the correct address" in {
    val pact = mock[Pact]
    Publisher.addressGenerator(details) shouldBe "someAddress/pacts/provider/someProvider/consumer/someConsumer/version/someVersion"
  }

  "Publisher" should "generate an address and send the json represented by a pact to the http service" in {
    val publisher = new Publisher(mock[HttpService], mock[PublishAddressGenerator], mock[PactFormatter])
    import publisher._
    when(addressGenerator.apply(details)) thenReturn "someAddress"
    when(pactFormatter.apply(pact)) thenReturn "someJson"
    val captor = ArgumentCaptor.forClass(classOf[HttpRequest])
    val response = mock[HttpResponse[String]]
    when(httpService.apply(captor.capture())) thenReturn response
    publisher(details)
    val actualRequest = captor.getValue
    actualRequest.headers should contain ("Content-Type", "application/json")
    actualRequest.url shouldBe "someAddress"
    actualRequest.method shouldBe "PUT"
    //would like to check that the json was put in the request but that's hard
  }

  it should "have an apply method with sensible functions" in {
    val publisher = Publisher()
    publisher.httpService shouldBe Publisher.httpService
    publisher.pactFormatter shouldBe Publisher.pactFormatter
    publisher.addressGenerator shouldBe Publisher.addressGenerator
  }

}
