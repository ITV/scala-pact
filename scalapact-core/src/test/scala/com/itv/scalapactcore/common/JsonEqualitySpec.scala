package com.itv.scalapactcore.common

import org.scalatest.{FunSpec, Matchers}
import argonaut._
import Argonaut._
import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.matching.ScalaPactJsonEquality._

class JsonEqualitySpec extends FunSpec with Matchers {

  describe("Equal json objects") {

    it("should prove equal json objects match") {

      val a = """{"name":"joe","age":23}"""
      val b = """{"age":23,"name":"joe"}"""
      val c = """{"age":23,"name":"josh"}"""

      a == b shouldEqual false

      withClue("optional") {
        val personA = a.parseOption
        val personB = b.parseOption
        val personC = c.parseOption

        (personA.get =~ personB.get)(None) shouldEqual true
        (personA.get =~ personC.get)(None) shouldEqual false
      }

    }

    it("should prove json arrays match") {
      val a = """[{"name":"joe","age":23},{"name":"maggie","age":31}]"""
      val b = """[{"name":"maggie","age":31},{"age":23,"name":"joe"}]"""

      a == b shouldEqual false

      withClue("optional") {
        val personA = a.parseOption
        val personB = b.parseOption

        (personA.get =~ personB.get)(None) shouldEqual true
      }

    }

    it("Should be able to spot different types") {

      val a = """{"name":"joe","age":23}"""
      val b = """[{"name":"joe","age":23}]"""

      (a.parseOption.get =~ b.parseOption.get)(None) shouldEqual false


      val c = """{"id":"123"}"""
      val d = """{"id":123}"""

      (c.parseOption.get =~ d.parseOption.get)(None) shouldEqual false
    }

    it("should be able to handle / ignore extra / missing fields") {

      val a = """{"name":"joe","age":23}"""
      val b = """{"age":23,"name":"joe","location":"London"}"""

      val personA = a.parseOption
      val personB = b.parseOption

      (personA.get =~ personB.get)(None) shouldEqual true
      (personB.get =~ personA.get)(None) shouldEqual false
    }

    it("should be able to handle more complex structures") {

      val a =
        """
          |{
          |  "name":"joe",
          |  "age":23,
          |  "friends" : [
          |    {"name":"josh","age":34},
          |    {"name":"maggie","age":31}
          |  ],
          |  "favouriteNumbers" : [
          |    1, 7, 100
          |  ]
          |}
        """.stripMargin

      val b =
        """
          |{
          |  "age":23,
          |  "name":"joe",
          |  "friends" : [
          |    {"name":"josh","age":34},
          |    {"age":31,"name":"maggie"}
          |  ],
          |  "favouriteNumbers" : [
          |    1, 100, 7
          |  ]
          |}
        """.stripMargin

      withClue("Equal but fields out of order") {
        (a.parseOption.get =~ b.parseOption.get)(None) shouldEqual true
      }

      val c =
        """
          |{
          |  "name":"joe",
          |  "age":23,
          |  "eyeColour":"brown",
          |  "friends" : [
          |    {"name":"josh","age":34},
          |    {"name":"maggie","age":31},
          |    {"name":"sarah","age":24,"eyeColour":"green"}
          |  ],
          |  "favouriteNumbers" : [
          |    1, 7, 100, 13
          |  ]
          |}
        """.stripMargin

      withClue("Additional / missing data") {
        (a.parseOption.get =~ c.parseOption.get)(None) shouldEqual true
        (c.parseOption.get =~ a.parseOption.get)(None) shouldEqual false
      }

    }

    it("should be able to handle favourite colours in wrong order") {
      val a =
        """
          |{"alligator":{
          |  "favouriteColours": ["blue", "red"]
          |}}
        """.stripMargin

      val b =
        """
          |{"alligator":{
          |  "favouriteColours": ["red","blue"]
          |}}
        """.stripMargin

      (a.parseOption.get =~ b.parseOption.get)(None) shouldEqual true
    }

  }

  describe("Specific cases") {

    it("should cope with case #1") {

      val a =
        """
          |{
          |  "x": {
          |    "y": [
          |      {
          |        "myDateTime": {
          |          "at": "2016-12-18T21:00Z"
          |        }
          |      },
          |      {
          |        "myDateTime": {
          |          "at": "2017-01-11T00:01Z"
          |        }
          |      }
          |    ]
          |  }
          |}
        """.stripMargin

      val b =
        """
          |{
          |  "x": {
          |    "y": [
          |      {
          |        "myDateTime": {
          |          "at": "2016-12-18T21:00Z"
          |        }
          |      },
          |      {
          |        "myDateTime": {
          |          "at": "2017-01-11T00:01Z"
          |        }
          |      }
          |    ]
          |  }
          |}
        """.stripMargin

      val regex = "^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2})(:(\\d{2})){0,1}(Z|\\+0100)$"

      val rules: Option[Map[String, MatchingRule]] =
        Option(
          Map(
            "$.body.x.y[*].myDateTime.at" -> MatchingRule(Option("regex"), Option(regex), None)
          )
        )

      withClue("Empty A") {
        ("{}".parseOption.get =~ b.parseOption.get) (rules) shouldEqual true
      }

      withClue("No rules") {
        (a.parseOption.get =~ b.parseOption.get)(None) shouldEqual true
      }

      withClue("The regex is valid") {
        "2016-12-18T21:00Z".matches(regex) shouldEqual true
      }

      withClue("A equals B and rule checked") {
       (a.parseOption.get =~ b.parseOption.get) (rules) shouldEqual true // Failure was here originally
      }

    }

  }

}

