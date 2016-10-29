package com.itv.scalapact.plugin.tester

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}


class PactFileWriterSpec extends FlatSpec with Matchers{

  "ScalaPactContractWriter.simplifyname" should "simplify names" in {
    PactFileWriter.simplifyName("someName") shouldBe "someName"
    PactFileWriter.simplifyName("som%&%&^%eNam_e") shouldBe "someName"
    PactFileWriter.simplifyName("some  Name") shouldBe "some--Name"
  }

}

