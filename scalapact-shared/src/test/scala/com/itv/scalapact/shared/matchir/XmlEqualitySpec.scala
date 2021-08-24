package com.itv.scalapact.shared.matchir

import com.itv.scalapact.shared.MatchingRule
import com.itv.scalapact.shared.matchir.IrNodeEqualityResult.{IrNodesEqual, IrNodesNotEqual}

import scala.xml.Elem
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class XmlEqualitySpec extends AnyFunSpec with Matchers {

  implicit class ElemOps(val elem: Elem) {
    def toNode = MatchIr.fromXml(elem)
  }

  def check(res: IrNodeEqualityResult): Unit =
    res match {
      case p: IrNodesEqual.type =>
        ()

      case e: IrNodesNotEqual =>
        fail(e.renderDifferences)
    }

  describe("testing the equality of xml objects") {

    it("should find equality of a simple example") {

      val expected = <fish><type>cod</type><side/></fish>.toNode
      val received = <fish><type>cod</type><side/></fish>.toNode

      check(expected =~ received)

    }

    it("should not find equality of a simple unequal example") {

      val expected2 = <fish><type>cod</type><side>chips</side></fish>.toNode
      val received2 = <fish><type>cod</type><side/></fish>.toNode

      withClue("Unequal") {
        (expected2 =~ received2).isEqual shouldEqual false
      }

    }

    it("should find equality when the right contains the left example") {

      val expected = <ns:fish battered="true"><type sustainable="false">cod</type><side>chips</side></ns:fish>.toNode
      val received =
        <ns:fish battered="true"><type sustainable="false" oceananic="true">cod</type><side>chips</side><sauce>ketchup</sauce></ns:fish>.toNode

      check(expected =~ received)

    }

    it("should not find equality when namespaces do not match") {

      // Note, the <sid> tag *is* equal since the left has less information than the right.
      val expected = <ns:fish><type>haddock</type><side/></ns:fish>.toNode
      val received = <fish><type>haddock</type><side>chips</side></fish>.toNode

      (expected =~ received).isEqual shouldEqual false

    }

  }

  describe("testing the equality of xml objects with matching rules") {

    it("should be able to accept equality with a rule in a simple case 1") {
      val expected = <fish><type>haddock</type></fish>.toNode
      val received = <fish><type>cod</type></fish>.toNode

      val rules: Option[Map[String, MatchingRule]] = Option {
        Map(
          ".fish.type" -> MatchingRule(Option("regex"), Some("haddock|cod"), None)
        )
      }

      IrNodeMatchingRules.fromPactRules(rules) match {
        case Left(e) =>
          fail(e)

        case Right(r) =>
          implicit val irRules: IrNodeMatchingRules = r
          check(expected =~ received)
      }

    }

    it("should be able to accept equality with a rule in a simple case with a sub element") {
      val expected = <fish><type>haddock</type><side>peas</side></fish>.toNode
      val received = <fish><type>haddock</type><side>chips</side></fish>.toNode

      val rules: Option[Map[String, MatchingRule]] = Option {
        Map(
          ".fish.side" -> MatchingRule(Option("type"), None, None)
        )
      }

      IrNodeMatchingRules.fromPactRules(rules) match {
        case Left(e) =>
          fail(e)

        case Right(r) =>
          implicit val irRules: IrNodeMatchingRules = r
          check(expected =~ received)
      }
    }

    it("should be able to handle a more complicated match") {

      val rules: Option[Map[String, MatchingRule]] = Option {
        Map(
          ".fish.breed" -> MatchingRule(Option("type"), None, None),
          ".fish.side"  -> MatchingRule(Option("regex"), Some("peas|chips"), None)
        )
      }

      IrNodeMatchingRules.fromPactRules(rules) match {
        case Left(e) =>
          fail(e)

        case Right(r) =>
          implicit val irRules: IrNodeMatchingRules = r

          val expected = <fish><breed>haddock</breed><side>peas</side></fish>.toNode
          val received = <fish><breed>cod</breed><side>chips</side></fish>.toNode
          check(expected =~ received)

          val expected2 = <fish><breed>haddock</breed><side>peas</side></fish>.toNode
          val received2 = <fish><breed>1</breed><side>chips</side></fish>.toNode
          (expected2 =~ received2).isEqual shouldEqual false

          val expected3 = <fish><breed>haddock</breed><side>peas</side></fish>.toNode
          val received3 = <fish><breed>cod</breed><side>hamburgers</side></fish>.toNode
          (expected3 =~ received3).isEqual shouldEqual false

          val expected4 = <fish><breed>haddock</breed><side>peas</side><sauce>ketchup</sauce></fish>.toNode
          val received4 = <fish><breed>cod</breed><side>chips</side><sauce>ketchup</sauce></fish>.toNode
          check(expected4 =~ received4)

          val expected5 = <fish><breed>haddock</breed><side>peas</side><sauce>ketchup</sauce></fish>.toNode
          val received5 = <fish><breed>cod</breed><side>chips</side><sauce>brown</sauce></fish>.toNode
          (expected5 =~ received5).isEqual shouldEqual false
      }
    }

  }

}
