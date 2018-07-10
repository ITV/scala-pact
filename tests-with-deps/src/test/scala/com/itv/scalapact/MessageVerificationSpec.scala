package com.itv.scalapact

import com.itv.scalapact.ScalaPactVerify.{pactAsJsonString, verifyPact}
import com.itv.scalapact.shared.MessageContentType.ApplicationJson
import com.itv.scalapact.shared.{Message, Pact, PactActor}
import org.scalatest.{Assertion, FlatSpec, Matchers, OptionValues}
import Matchers._
import com.itv.scalapact.shared.matchir.IrNode
import com.itv.scalapact.shared.typeclasses.IPactReader
import org.scalactic.TypeCheckedTripleEquals

class MessageVerificationSpec extends FlatSpec with TypeCheckedTripleEquals with OptionValues {
  import json.{pactReaderInstance => _, _}
  import argonaut._
  import Argonaut._
  import com.itv.scalapact.argonaut62.PactImplicits._

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

  implicit val reader: IPactReader = new IPactReader {
    override def jsonStringToPact(json: String): Either[String, Pact] = Right(samplePact)

    override def fromJSON(jsonString: String): Option[IrNode] = ???
  }

  it should " be able to verify a simple contract" in {
    verifyPact
      .withPactSource(pactAsJsonString(samplePact.asJson.nospaces))
      .noSetupRequired
      .runMessageTests[Assertion] { r =>
        r.consume("Published credit data") { message =>
          message should ===(samplePact.messages.headOption.value)
        }
      }

  }
}
