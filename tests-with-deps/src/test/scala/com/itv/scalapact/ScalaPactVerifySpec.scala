package com.itv.scalapact

import org.scalatest.{FunSpec, Matchers}
import com.itv.scalapact.ScalaPactVerify.VerifyTargetConfig

import scala.concurrent.duration._

class ScalaPactVerifySpec extends FunSpec with Matchers {

  describe("The Pact Verify functions") {
    it("Should be able to create a VerifyTargetConfig from a url") {

      VerifyTargetConfig.fromUrl("http://localhost:1234").get shouldEqual VerifyTargetConfig(
        "http",
        "localhost",
        1234,
        2.seconds
      )
      VerifyTargetConfig.fromUrl("https://localhost:1234/blah/nonsense123/987").get shouldEqual VerifyTargetConfig(
        "https",
        "localhost",
        1234,
        2.seconds
      )
      VerifyTargetConfig.fromUrl("fish") shouldEqual None

    }
  }

}
