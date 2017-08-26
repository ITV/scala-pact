package com.itv.scalapactcore.common.matchir

import org.scalatest.{FunSpec, Matchers}

class IrEqualitySpec extends FunSpec with Matchers {

//  describe("strictly matching structures") {
//
//    it("should make a nice tree...") {
//
//      val jsonA: String =
//        """
//          |{
//          |  "river": [
//          |    {
//          |      "fish": {
//          |        "breed": "haddock"
//          |      }
//          |    }
//          |  ],
//          |  "riverbank": {
//          |    "grassy": true,
//          |    "flowers": [ "poppies", "daisies", "dandelions" ]
//          |  },
//          |  "another_riverbank": {
//          |    "grassy": false,
//          |    "flowers": [ "daisies", "dandelions", "poppies" ]
//          |  }
//          |}
//        """.stripMargin
//
//      val jsonB: String =
//        """
//          |{
//          |  "river": [
//          |    {
//          |      "fish": {
//          |        "breed": "cod"
//          |      }
//          |    },
//          |    {
//          |      "fish": {
//          |        "breed": "haddock"
//          |      }
//          |    }
//          |  ],
//          |  "riverbank": {
//          |    "grassy": true,
//          |    "flowers": [ "poppies", "daisies" ]
//          |  },
//          |  "another_riverbank": {
//          |    "grassy": false,
//          |    "flowers": [ "poppies", "daisies", "dandelions" ]
//          |  }
//          |}
//        """.stripMargin
//
//      val a = MatchIr.fromJSON(jsonA).get
//      val b = MatchIr.fromJSON(jsonB).get
//
//      val res = IrNodeTreePair.combine(a, b).renderAsString
//
//      println(res)
//
//      fail()
//    }

//    it("should check simple equality") {
//
//      val a = IrNode("fish", IrNode("breed", IrStringNode("cod")))
//      val b = IrNode("fish", IrNode("breed", IrStringNode("haddock")))
//
//      a shouldEqual a
//      a shouldNot be(b)
//
//    }

//  }

}