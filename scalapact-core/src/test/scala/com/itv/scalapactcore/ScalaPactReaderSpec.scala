package com.itv.scalapactcore

import org.scalatest.{FunSpec, Matchers}

class ScalaPactReaderSpec extends FunSpec with Matchers {

  describe("Reading a Heterogeneous Pact file") {

    it("should be able to read Pact files with an arbitrary body"){

      ScalaPactReader.jsonStringToPact(PactFileExamples.simpleAsString) shouldEqual PactFileExamples.simple

    }

  }

}

