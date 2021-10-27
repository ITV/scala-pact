package com.itv.scalapact.circe14

import io.circe.parser._
import org.scalatest.OptionValues
import com.itv.scalapact.test.PactFileExamples
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ScalaPactReaderWriterSpec extends AnyFunSpec with Matchers with OptionValues {

  val pactReader = new PactReader
  val pactWriter = new PactWriter

  val scalaPactVersion: String = "1.0.0"

  describe("Reading and writing a homogeneous Pact files") {

    it("should be able to read Pact files") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.simpleAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simple
    }

    it("should be able to read Pact files using the old provider state key") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.simpleOldProviderStateAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simple
    }

    it("should be able to write Pact files") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple, scalaPactVersion)

      val expected = PactFileExamples.simpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

    it("should be able to eat it's own dog food") {

      val json = pactWriter.pactToJsonString(PactFileExamples.simple, scalaPactVersion)

      val pact = pactReader.jsonStringToScalaPact(json).toOption.value

      val `reJson'd` = parse(pactWriter.pactToJsonString(pact, scalaPactVersion)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.simpleAsString).toOption.get

      pact shouldEqual PactFileExamples.simple

    }

    it("should be able to read ruby format json") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.simpleAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simple
    }

    it("should be able to write a pact file in ruby format") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple, scalaPactVersion)

      val expected = PactFileExamples.simpleAsString

      written shouldEqual expected

    }

    it("should be able to eat it's own dog food with no body") {

      val json = pactWriter.pactToJsonString(PactFileExamples.verySimple, scalaPactVersion)

      val pact = pactReader.jsonStringToScalaPact(json).toOption.value

      val `reJson'd` = parse(pactWriter.pactToJsonString(pact, scalaPactVersion)).toOption.get

      `reJson'd` shouldEqual parse(PactFileExamples.verySimpleAsString).toOption.get

      pact shouldEqual PactFileExamples.verySimple

    }

    it("should be able to read ruby format json with no body") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.verySimpleAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.verySimple
    }

    it("should be able to write a pact file in ruby format with no body") {

      val written = pactWriter.pactToJsonString(PactFileExamples.verySimple, scalaPactVersion)

      val expected = PactFileExamples.verySimpleAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }

    it("should be able to parse another example") {

      pactReader.jsonStringToScalaPact(PactFileExamples.anotherExample) match {
        case Left(e) =>
          fail(e)

        case Right(pact) =>
          pact.consumer.name shouldEqual "My Consumer"
          pact.provider.name shouldEqual "Their Provider Service"
          pact.interactions.head.response.body.get shouldEqual "Hello there!"
      }

    }

    it("should be able to parse _links and metadata") {
      val pactEither = pactReader.jsonStringToScalaPact(PactFileExamples.simpleWithLinksAndMetaDataAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simpleWithLinksAndMetaData
    }

    it("should remove curies and pb:consumer-versions from parsed _links") {
      val simpleWithCuriesAndPbConsumerVersionsAsString: String =
        """{
          |  "provider" : {
          |    "name" : "provider"
          |  },
          |  "consumer" : {
          |    "name" : "consumer"
          |  },
          |  "interactions" : [
          |    {
          |      "request" : {
          |        "method" : "GET",
          |        "body" : "fish",
          |        "path" : "/fetch-json",
          |        "matchingRules" : {
          |          "$.headers.Accept" : {
          |            "match" : "regex",
          |            "regex" : "\\w+"
          |          },
          |          "$.headers.Content-Length" : {
          |            "match" : "type"
          |          }
          |        },
          |        "query" : "fish=chips",
          |        "headers" : {
          |          "Content-Type" : "text/plain"
          |        }
          |      },
          |      "description" : "a simple request",
          |      "response" : {
          |        "status" : 200,
          |        "headers" : {
          |          "Content-Type" : "application/json"
          |        },
          |        "body" : {
          |          "fish" : [
          |            "cod",
          |            "haddock",
          |            "flying"
          |          ]
          |        },
          |        "matchingRules" : {
          |          "$.headers.Accept" : {
          |            "match" : "regex",
          |            "regex" : "\\w+"
          |          },
          |          "$.headers.Content-Length" : {
          |            "match" : "type"
          |          }
          |        }
          |      },
          |      "providerState" : "a simple state"
          |    },
          |    {
          |      "request" : {
          |        "method" : "GET",
          |        "body" : "fish",
          |        "path" : "/fetch-json2",
          |        "headers" : {
          |          "Content-Type" : "text/plain"
          |        }
          |      },
          |      "description" : "a simple request 2",
          |      "response" : {
          |        "status" : 200,
          |        "headers" : {
          |          "Content-Type" : "application/json"
          |        },
          |        "body" : {
          |          "chips" : true,
          |          "fish" : [
          |            "cod",
          |            "haddock"
          |          ]
          |        }
          |      },
          |      "providerState" : "a simple state 2"
          |    }
          |  ],
          |  "_links": {
          |    "self": {
          |      "title": "Pact",
          |      "name": "Pact between consumer (v1.0.0) and provider",
          |      "href": "http://localhost/pacts/provider/provider/consumer/consumer/version/1.0.0"
          |    },
          |    "pb:consumer": {
          |      "title": "Consumer",
          |      "name": "consumer",
          |      "href": "http://localhost/pacticipants/consumer"
          |    },
          |    "pb:provider": {
          |      "title": "Provider",
          |      "name": "provider",
          |      "href": "http://localhost/pacticipants/provider"
          |    },
          |    "pb:latest-tagged-pact-version": {
          |      "title": "Latest tagged version of this pact",
          |      "href": "http://localhost/pacts/provider/provider-service/consumer/consumer-service/latest/{tag}",
          |      "templated": true
          |    },
          |    "pb:consumer-versions": [
          |      {
          |        "title": "Consumer version",
          |        "name": "1.2.3",
          |        "href": "http://localhost/pacticipants/consumer/versions/1.2.3"
          |      }
          |    ],
          |    "curies": [
          |      {
          |        "name": "pb",
          |        "href": "http://localhost/doc/{rel}",
          |        "templated": true
          |      }
          |    ]
          |  },
          |  "metadata": {
          |    "pactSpecification": {
          |      "version": "2.0.0"
          |    },
          |    "scala-pact": {
          |      "version": "1.0.0"
          |    }
          |  }
          |}""".stripMargin

      val pactEither = pactReader.jsonStringToScalaPact(simpleWithCuriesAndPbConsumerVersionsAsString)

      pactEither.toOption.value shouldEqual PactFileExamples.simpleWithLinksAndMetaData
    }

    it("should be able to write Pact files and add metadata when missing") {

      val written = pactWriter.pactToJsonString(PactFileExamples.simple, scalaPactVersion)

      val expected = PactFileExamples.simpleWithMetaDataAsString

      parse(written).toOption.get shouldEqual parse(expected).toOption.get
    }
  }
}
