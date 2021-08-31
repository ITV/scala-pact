package com.example.consumer

import com.itv.scalapact.{ScalaPactMockConfig, ScalaPactMockServer}
import com.itv.scalapact.model.ScalaPactDescription
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

/** Stands up the stub service with all stubs prior to running tests and shuts it down afterwards. */
class SingletonStubProviderClientSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  // The import contains two things:
  // 1. The consumer test DSL/Builder
  // 2. Helper implicits, for instance, values will automatically be converted
  //    to Option types where the DSL requires it.
  import com.itv.scalapact.ScalaPactForger._

  // Import the json and http libraries specified in the build.sbt file
  import com.itv.scalapact.circe14._
  import com.itv.scalapact.http4s23._

  implicit val formats: DefaultFormats.type = DefaultFormats

  val CONSUMER = "scala-pact-consumer"
  val PROVIDER = "scala-pact-provider"

  val people = List("Bob", "Fred", "Harry")

  val body: String = write(
    Results(
      count = 3,
      results = people
    )
  )

  // Forge all pacts up front
  val pact: ScalaPactDescription = forgePact
    .between(CONSUMER)
    .and(PROVIDER)
    .addInteraction(
      interaction
        .description("Fetching results")
        .given("Results: Bob, Fred, Harry")
        .uponReceiving("/results")
        .willRespondWith(200, Map("Pact" -> "modifiedRequest"), body)
    )
    .addInteraction(
      interaction
        .description("Fetching least secure auth token ever")
        .uponReceiving(
          method = GET,
          path = "/auth_token",
          query = None,
          headers = Map("Accept" -> "application/json", "Name" -> "Bob"),
          body = None,
          matchingRules = // When stubbing (during this test or externally), we don't mind
            // what the name is, as long as it only contains letters.
            headerRegexRule("Name", "^([a-zA-Z]+)$")
        )
        .willRespondWith(
          status = 202,
          headers = Map("Content-Type" -> "application/json; charset=UTF-8"),
          body = Some("""{"token":"abcABC123"}"""),
          matchingRules = // When verifying externally, we don't mind what is in the token
            // as long as it contains a token field with an alphanumeric
            // value
            bodyRegexRule("token", "^([a-zA-Z0-9]+)$")
        )
    )

  lazy val server: ScalaPactMockServer = pact.startServer()
  lazy val config: ScalaPactMockConfig = server.config

  override def beforeAll(): Unit = {
    // Initialize the Pact stub server prior to tests executing.
    val _ = server
    ()
  }

  override def afterAll(): Unit = {
    // Shut down the stub server when tests are finished.
    server.stop()
  }

  describe("Connecting to the Provider service") {
    it("should be able to fetch results") {
      val results = ProviderClient.fetchResults(config.baseUrl)
      results.isDefined shouldEqual true
      results.get.count shouldEqual 3
      results.get.results.forall(p => people.contains(p)) shouldEqual true
    }

    it("should be able to get an auth token") {
      val token = ProviderClient.fetchAuthToken(config.host, config.port, "Sally")
      token.isDefined shouldEqual true
      token.get.token shouldEqual "abcABC123"
    }
  }
}
