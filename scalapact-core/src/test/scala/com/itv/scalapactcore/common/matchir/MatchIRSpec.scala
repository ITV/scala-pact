package com.itv.scalapactcore.common.matchir

import org.scalatest.{FunSpec, Matchers}

import scala.language.implicitConversions

class MatchIRSpec extends FunSpec with Matchers {

  implicit def toOption[A](v: A): Option[A] = Option(v)

  describe("Converting XML to MatchIR") {

    it("should be able to convert one node") {

      val xml: String = <fish></fish>.toString()

      val ir: Option[IrNode] = IrNode("fish", Map(), None, Nil)

      MatchIR.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert one node with content") {

      val xml: String = <fish>haddock</fish>.toString()

      val ir: Option[IrNode] = IrNode("fish", Map(), Some(IrStringNode("haddock")), Nil)

      MatchIR.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert one node with attributes") {

      val xml: String = <fish id="3" description="A fish" endangered="false"></fish>.toString()

      val ir: Option[IrNode] = IrNode(
        "fish",
        Map("id" -> IrIntNode(3), "description" -> IrStringNode("A fish"), "endangered" -> IrBooleanNode(false)),
        None,
        Nil
      )

      MatchIR.fromXml(xml) shouldEqual ir

    }

    it("should be able to convert two nested nodes and a value") {

      val xml: String = <fish><breed>cod</breed></fish>.toString()

      val ir: Option[IrNode] = IrNode("fish", Map(), None,
        List(
          IrNode("breed", Map(), Some(IrStringNode("code")), Nil)
        )
      )

      MatchIR.fromXml(xml) shouldEqual ir

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

      val ir = IrNode("", Map(), None,
        List(
          IrNode("fish", Map(), None, Nil)
        )
      )

      MatchIR.fromJSON(json) shouldEqual ir

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

      val ir = IrNode("", Map(), None,
        List(
          IrNode("fish", Map(), None,
            List(
              IrNode("breed", Map(), Some(IrStringNode("cod")), Nil)
            )
          )
        )
      )

      MatchIR.fromJSON(json) shouldEqual ir

    }

  }

}
