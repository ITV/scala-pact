package com.itv.scalapactcore.common.matchir

import org.scalatest.{FunSpec, Matchers}

import scala.language.implicitConversions

class MatchIrSpec extends FunSpec with Matchers {

  describe("Converting XML to MatchIR") {

    it("should be able to convert one node") {

      val xml: String = <fish></fish>.toString()

      val ir: Option[IrNode] = Option {
        IrNode("fish")
      }

      MatchIr.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert one node with content") {

      val xml: String = <fish>haddock</fish>.toString()

      val ir: Option[IrNode] = Option {
        IrNode("fish", IrStringNode("haddock"))
      }

      MatchIr.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert one node with a namespace") {

      val xml: String = <ns1:fish>haddock</ns1:fish>.toString()

      val ir: Option[IrNode] = Option {
        IrNode("fish", Some(IrStringNode("haddock")), Nil, Option("ns1"), Map.empty[String, IrNodePrimitive])
      }

      MatchIr.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert one node with attributes") {

      val xml: String = <fish id="3" description="A fish" endangered="false"></fish>.toString()

      val ir: Option[IrNode] = Option {
        IrNode("fish")
          .withAttributes(Map("id" -> IrNumberNode(3), "description" -> IrStringNode("A fish"), "endangered" -> IrBooleanNode(false)))
      }

      MatchIr.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert two nested nodes and a value") {

      val xml: String = <fish><breed>cod</breed></fish>.toString()

      val ir: Option[IrNode] = Option {
        IrNode("fish", IrNode("breed", IrStringNode("cod")))
      }

      MatchIr.fromXml(xml) shouldEqual ir

    }

  }

  describe("Converting JSON to MatchIR") {

    it("should be able to convert one node") {
      val json: String =
        """
          |{
          |  "fish": {}
          |}
        """.stripMargin

      val ir: Option[IrNode] = Option {
        IrNode(MatchIr.rootNodeLabel, IrNode("fish"))
      }

      MatchIr.fromJSON(json) shouldEqual ir
    }

    it("should be able to convert two nested nodes and a value") {

      val json: String =
        """
          |{
          |  "fish": {
          |    "breed": "cod"
          |  }
          |}
        """.stripMargin

      val ir: Option[IrNode] = Option {
        IrNode(
          MatchIr.rootNodeLabel,
          IrNode(
            "fish",
            IrNode(
              "breed",
              IrStringNode("cod")
            )
          )
        )
      }

      MatchIr.fromJSON(json) shouldEqual ir

    }

    it("should convert a simple top level array") {

      val json: String =
        """
          |[1,2,3]
        """.stripMargin

      val ir: Option[IrNode] = Option {
        IrNode(
          MatchIr.rootNodeLabel,
          IrNode(MatchIr.unnamedNodeLabel, IrNumberNode(1)),
          IrNode(MatchIr.unnamedNodeLabel, IrNumberNode(2)),
          IrNode(MatchIr.unnamedNodeLabel, IrNumberNode(3))
        )
      }

      MatchIr.fromJSON(json) shouldEqual ir

    }

    it("should be able to convert a top level array with two nodes") {

      val json: String =
        """
          |[
          |  {
          |    "fish": {
          |      "breed": "cod"
          |    }
          |  },
          |  {
          |    "fish": {
          |      "breed": "haddock"
          |    }
          |  }
          |]
        """.stripMargin

      val ir: Option[IrNode] = Option {
        IrNode(MatchIr.rootNodeLabel,
          IrNode(
            MatchIr.unnamedNodeLabel,
            IrNode(
              "fish",
              IrNode("breed", IrStringNode("cod"))
            )
          ),
          IrNode(
            MatchIr.unnamedNodeLabel,
            IrNode(
              "fish",
              IrNode("breed", IrStringNode("haddock"))
            )
          )
        )
      }

//      println(ir.map(_.renderAsString).getOrElse("OOPS"))

      MatchIr.fromJSON(json) shouldEqual ir

    }

    it("should be able to convert a nested json structure with a nested array") {

      val json: String =
        """
          |{
          |  "river": [
          |    {
          |      "fish": {
          |        "breed": "cod"
          |      }
          |    },
          |    {
          |      "fish": {
          |        "breed": "haddock"
          |      }
          |    }
          |  ],
          |  "riverbank": {
          |    "grassy": true,
          |    "flowers": [ "poppies", "daisies", "dandelions" ]
          |  }
          |}
        """.stripMargin

      val expected: Option[IrNode] = Option {

        IrNode(
          MatchIr.rootNodeLabel,
          IrNode(
            "river",
            IrNode(
              "river",
              IrNode(
                "fish",
                IrNode("breed", IrStringNode("cod"))
              )
            ),
            IrNode(
              "river",
              IrNode(
                "fish",
                IrNode("breed", IrStringNode("haddock"))
              )
            )
          ),
          IrNode(
            "riverbank",
            IrNode(
              "grassy",
              IrBooleanNode(true)
            ),
            IrNode(
              "flowers",
              IrNode(
                "flowers",
                IrStringNode("poppies")
              ),
              IrNode(
                "flowers",
                IrStringNode("daisies")
              ),
              IrNode(
                "flowers",
                IrStringNode("dandelions")
              )
            )
          )
        )

      }

//      println("Expected: ")
//      println(expected.map(_.renderAsString).getOrElse("OOPS: Failed to render 'expected' as string"))

      val actual = MatchIr.fromJSON(json)

//      println("Actual: ")
//      println(actual.map(_.renderAsString).getOrElse("OOPS: Failed to render 'actual' as string"))

      actual shouldEqual expected

    }

  }

}
