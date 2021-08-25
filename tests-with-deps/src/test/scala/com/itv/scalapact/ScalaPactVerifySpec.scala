package com.itv.scalapact

import com.itv.scalapact.ScalaPactVerify.VerifyTargetConfig

import scala.concurrent.duration._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ScalaPactVerifySpec extends AnyFunSpec with Matchers {

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
