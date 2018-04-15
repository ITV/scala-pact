package com.itv.scalapactcore.common

import argonaut.Argonaut._
import argonaut._
import com.itv.scalapact.argonaut62.JsonConversionFunctions
import com.itv.scalapact.shared.matchir._
import org.scalatest.{FunSpec, Matchers}

import scala.language.implicitConversions

class JsonEqualitySpec extends FunSpec with Matchers {

  implicit def elemToNode(json: Json): IrNode =
    MatchIr.fromJSON(JsonConversionFunctions.fromJSON)(json.toString()).get

  def check(res: IrNodeEqualityResult): Unit =
    res match {
      case p @ IrNodesEqual   => p shouldEqual IrNodesEqual
      case e: IrNodesNotEqual => fail(e.renderDifferences)
    }

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

        check(personA.get =~ personB.get)
        (personA.get =~ personC.get).isEqual shouldEqual false
      }

    }

    it("should prove json arrays match") {
      val a = """[{"name":"joe","age":23},{"name":"maggie","age":31}]"""
      val b = """[{"name":"maggie","age":31},{"age":23,"name":"joe"}]"""

      a == b shouldEqual false

      withClue("optional") {
        val personA = a.parseOption
        val personB = b.parseOption

        check(personA.get =~ personB.get)
      }

    }

    it("Should be able to spot different types") {

      val a = """{"name":"joe","age":23}"""
      val b = """[{"name":"joe","age":23}]"""

      (a.parseOption.get =~ b.parseOption.get).isEqual shouldEqual false

      val c = """{"id":"123"}"""
      val d = """{"id":123}"""

      (c.parseOption.get =~ d.parseOption.get).isEqual shouldEqual false
    }

    it("should be able to handle / ignore extra / missing fields") {

      val a = """{"name":"joe","age":23}"""
      val b = """{"age":23,"name":"joe","location":"London"}"""

      val personA = a.parseOption
      val personB = b.parseOption

      check(personA.get =~ personB.get)
      (personB.get =~ personA.get).isEqual shouldEqual false
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
        check(a.parseOption.get =~ b.parseOption.get)
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
        check(a.parseOption.get =~ c.parseOption.get)
        (c.parseOption.get =~ a.parseOption.get).isEqual shouldEqual false
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

      check(a.parseOption.get =~ b.parseOption.get)
    }

  }

}
