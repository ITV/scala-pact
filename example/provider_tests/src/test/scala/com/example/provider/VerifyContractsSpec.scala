package com.example.provider

import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import com.itv.scalapact.ScalaPactVerify._
import com.itv.scalapact.shared.ProviderStateResult

class VerifyContractsSpec extends FunSpec with Matchers with BeforeAndAfterAll {

  import com.itv.scalapact.circe13._
  import com.itv.scalapact.http4s21._

  val serverAllocated =
    AlternateStartupApproach.serverResource(_ => List("Bob", "Fred", "Harry"), _ => "abcABC123").allocated.unsafeRunSync()

  override def beforeAll(): Unit = {
    ()
  }

  override def afterAll(): Unit = {
    serverAllocated._2.unsafeRunSync()
  }


  describe("Verifying Consumer Contracts") {
    it("should be able to verify it's contracts") {
      verifyPact
        .withPactSource(loadFromLocal("delivered_pacts"))
        .setupProviderState("given") {
          case "Results: Bob, Fred, Harry" =>
            val newHeader = "Pact" -> "modifiedRequest"
            ProviderStateResult(true, req => req.copy(headers = Option(req.headers.fold(Map(newHeader))(_ + newHeader))))
        }
        .runVerificationAgainst("localhost", 8080)
    }
  }

}
