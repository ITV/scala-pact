package com.example.consumer

import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import org.scalatest.{FunSpec, Matchers}

class ProviderClientSpec extends FunSpec with Matchers {

  // The import contains two things:
  // 1. The consumer test DSL/Builder
  // 2. Helper implicits, for instance, values will automatically be converted
  //    to Option types where the DSL requires it.
  import com.itv.scalapact.ScalaPactForger._

  implicit val formats = DefaultFormats

  describe("Connecting to the Provider service") {

    it("should be able to fetch results"){

      val people = List("Bob", "Fred", "Harry")

      val body = write(
        Results(
          count = 3,
          results = people
        )
      )

      forgePact
        .between("Consumer")
        .and("Provider")
        .addInteraction(
          interaction
            .description("Fetching results")
            .given("Results: Bob, Fred, Harry")
            .uponReceiving("/results")
            .willRespondWith(200, body)
        )
        .runConsumerTest { mockConfig =>

          val results = ProviderClient.fetchResults(mockConfig.baseUrl)

          results.isDefined shouldEqual true
          results.get.count shouldEqual 3
          results.get.results.forall(p => people.contains(p)) shouldEqual true

        }

    }

    it("should be able to get an auth token") {
      forgePact
        .between("Consumer")
        .and("Provider")
        .addInteraction(
          interaction
            .description("Fetching least secure auth token ever")
            .uponReceiving(
              method = GET,
              path = "/auth_token",
              query = None,
              headers = Map("Accept" -> "application/json", "Name" -> "Bob"),
              body = None,
              matchingRules =
                // When stubbing (during this test or externally), we don't mind
                // what the name is, as long as it only contains letters.
                headerRegexRule("Name", "^([a-zA-Z]+)$")
            ).willRespondWith(
              status = 202,
              headers = Map("Content-Type" -> "application/json; charset=UTF-8"),
              body = """{"token":"abcABC123"}""",
              matchingRules =
                // When verifying externally, we don't mind what is in the token
                // as long as it contains a token field with an alphanumeric
                // value
                bodyRegexRule("token", "^([a-zA-Z0-9]+)$")
            )
        )
        .runConsumerTest { mockConfig =>
          val token = ProviderClient.fetchAuthToken(mockConfig.host, mockConfig.port, "Sally")

          token.isDefined shouldEqual true
          token.get.token shouldEqual "abcABC123"
        }
    }

  }

}
