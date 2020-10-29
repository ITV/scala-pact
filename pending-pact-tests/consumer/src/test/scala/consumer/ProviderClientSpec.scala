package consumer

import java.io.File

import com.itv.scalapact.PactForgerSuite
import org.scalatest.{FunSpecLike, Matchers}

import scala.io.{BufferedSource, Source}

class ProviderClientSpec extends PactForgerSuite with FunSpecLike with Matchers {

  val CONSUMER = "scala-pact-pending-test-consumer"
  val PROVIDER = "scala-pact-pending-test-provider"

  describe("Connecting to the Provider service") {

    it("should be able to fetch results") {
      val expectedBody = {
        val tag = {
          val s = Source.fromFile(new File("tag.txt"))
          val t = s.getLines().mkString
          s.close()
          t
        }
        val bodySource: BufferedSource = tag match {
          case "master" => Source.fromFile(new File("master-expected.json"))
          case "test" => Source.fromFile(new File("test-expected.json"))
          case x => throw new Exception(s"Unexpected tag value: $x")
        }
        val body = bodySource.getLines().mkString
        bodySource.close()
        body
      }

      forgePact
        .between(CONSUMER)
        .and(PROVIDER)
        .addInteraction(
          interaction
            .description("Fetching results")
            .given("")
            .uponReceiving("/results")
            .willRespondWith(200, expectedBody)
        )
        .runConsumerTest(_ => ())
    }

    it("an updated pact for a different consumer") {
      val expectedBody = {
        val bodySource = Source.fromFile(new File("master-expected.json"))
        val body = bodySource.getLines().mkString
        bodySource.close()
        body
      }

      forgePact
        .between(CONSUMER + "2")
        .and(PROVIDER)
        .addInteraction(
          interaction
            .description("Fetching results with updated expectations")
            .given("")
            .uponReceiving("/results2")
            .willRespondWith(200, expectedBody)
        )
        .runConsumerTest(_ => ())
    }
  }

}
