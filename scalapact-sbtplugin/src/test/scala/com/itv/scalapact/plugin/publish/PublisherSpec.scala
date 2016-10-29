package com.itv.scalapact.plugin.publish

import com.itv.scalapact.plugin.tester.PactFixture
import com.itv.scalapact.plugin.{HttpService, PactFormatter, PublishAddressGenerator}
import com.itv.scalapactcore.{Pact, PactActor}
import org.mockito.ArgumentCaptor
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

import scalaj.http.{HttpRequest, HttpResponse}
import scalaz.{-\/, \/-}


class PublisherSpec extends FlatSpec with Matchers with MockitoSugar with PactFixture {

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
    actualRequest.headers should contain("Content-Type", "application/json")
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


  it should "have a pactToDetails method that turns a pact into PublishDetails" in {
    Publisher.pactToDetails("http://some.address", "someVersion")(pact123) shouldBe \/-(PublishDetails("http://some.address", pact123.provider.name, pact123.consumer.name, "someVersion", pact123))
    Publisher.pactToDetails("", "someVersion")(pact123) shouldBe -\/("Pact broker address not set, you need to add the following line to you SBT file: \npactBrokerAddress := \"http://pactbroker.myserver.com\"")
    Publisher.pactToDetails("some.address", "someVersion")(pact123) shouldBe -\/("Pact broker address does not appear to be valid, should start with 'http(s)' protocol.")
    Publisher.pactToDetails("http://&*()&", "someVersion")(pact123) shouldBe -\/("Pact broker address does not appear to be valid, should be of form: http://my.broker-address.com")
  }

  it should "have a pactToDetails method that urlEncodes the consumer" in {
    val \/-(PublishDetails(_, _, consumerName, _, _)) = Publisher.pactToDetails("http://some.address", "someVersion")(pact123.copy(consumer = PactActor("qwe!@£")))
    consumerName shouldBe "qwe!%40%C2%A3"
  }
  it should "have a pactToDetails method that urlEncodes the provider" in {
    val \/-(PublishDetails(_, providerName, _, _, _)) = Publisher.pactToDetails("http://some.address", "someVersion")(pact123.copy(provider = PactActor("qwe!@£")))
    providerName shouldBe "qwe!%40%C2%A3"
  }

}
