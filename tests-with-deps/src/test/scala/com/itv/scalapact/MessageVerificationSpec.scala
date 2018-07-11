package com.itv.scalapact

import com.itv.scalapact.ScalaPactVerify.{ScalaPactVerifyFailed, pactAsJsonString, pactBroker, verifyPact}
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared._
import org.scalatest.{Assertion, FlatSpec, Matchers, OptionValues}
import Matchers._
import com.itv.scalapact.shared.typeclasses.IScalaPactHttpClient
import fs2.Task
import org.scalactic.TypeCheckedTripleEquals

import scala.concurrent.duration.Duration

class MessageVerificationSpec extends FlatSpec with TypeCheckedTripleEquals with OptionValues {

  import json._
  import argonaut._
  import Argonaut._
  import com.itv.scalapact.argonaut62.PactImplicits._

  val samplePactUrl = "http://pact.itv.com"
  val samplePact = Pact(
    consumer = PactActor("Consumer"),
    provider = PactActor("Provider"),
    interactions = List.empty,
    messages = List(
      Message(
        description = "Published credit data",
        providerState = Some("or maybe 'scenario'? not sure about this"),
        contents = """{"foo":"bar"}""",
        metaData = Message.Metadata("contentType" -> "application/json"),
        matchingRules = Map.empty,
        ApplicationJson
      )
    )
  )
  implicit val httpClient: IScalaPactHttpClient[Task] = mockHttpClient

  it should " be able to verify a simple contract from pact source as json string" in {
    verifyPact
      .withPactSource(pactAsJsonString(samplePact.asJson.nospaces))
      .noSetupRequired
      .runMessageTests[Task, Assertion]() {
        _.consume("Published credit data") { message =>
          message should ===(samplePact.messages.headOption.value)
        }
      }
  }

  it should "be able to verify a simple contract from pact broker" in {
    verifyPact
      .withPactSource(pactBroker(samplePactUrl, "Provider", List("Consumer")))
      .noSetupRequired
      .runMessageTests[Task, Assertion]() {
        _.consume("Published credit data") { message =>
          message should ===(samplePact.messages.headOption.value)
        }
      }
  }

  it should "fail to verify a simple contract from pact broker when the pact broker url does not exist" in {
    a[ScalaPactVerifyFailed] should be thrownBy {
      verifyPact
        .withPactSource(pactBroker("http://mypacts.json", "Provider", List("Consumer")))
        .noSetupRequired
        .runMessageTests[Task, Assertion]() {
          _.consume("Published credit data") { message =>
            message should ===(samplePact.messages.headOption.value)
          }
        }
    }
  }

  private def mockHttpClient =
    new IScalaPactHttpClient[Task] {
      override def doRequest(
          simpleRequest: SimpleRequest
      )(implicit sslContextMap: SslContextMap): Task[SimpleResponse] = ???

      override def doInteractionRequest(
          url: String,
          ir: InteractionRequest,
          clientTimeout: Duration,
          sslContextName: Option[String]
      )(implicit sslContextMap: SslContextMap): Task[InteractionResponse] = ???

      override def doRequestSync(simpleRequest: SimpleRequest)(
          implicit sslContextMap: SslContextMap
      ): Either[Throwable, SimpleResponse] = simpleRequest match {
        case s if s.baseUrl.startsWith(samplePactUrl) =>
          Right(SimpleResponse(200, Map.empty, Some(samplePact.asJson.nospaces)))
        case _ => Right(SimpleResponse(404, Map.empty, None))
      }

      override def doInteractionRequestSync(
          url: String,
          ir: InteractionRequest,
          clientTimeout: Duration,
          sslContextName: Option[String]
      )(implicit sslContextMap: SslContextMap): Either[Throwable, InteractionResponse] = ???
    }
}
