package com.itv.scalapactcore.common

import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.matchir.{IrNodeEqualityResult, IrNodesEqual, IrNodesNotEqual}
import com.itv.scalapactcore.common.matchir.MatchIrConverters._
import org.scalatest.{FunSpec, Matchers}

class XmlEqualitySpec extends FunSpec with Matchers {

  def check(res: IrNodeEqualityResult): Unit =
    res match {
      case p @ IrNodesEqual => p shouldEqual IrNodesEqual
      case e: IrNodesNotEqual => fail(e.renderDifferences)
    }

  describe("testing the equality of xml objects") {

    it("should find equality of a simple example") {

      val expected = <fish><type>cod</type><side/></fish>
      val received = <fish><type>cod</type><side/></fish>

      check(expected =~ received)

    }

    it("should not find equality of a simple unequal example") {

      // Note, the <side> tag *is* equal since the left has less information than the right.
      val expected = <fish><type>haddock</type><side/></fish>
      val received = <fish><type>haddock</type><side>chips</side></fish>

      check(expected =~ received)

      val expected2 = <fish><type>cod</type><side>chips</side></fish>
      val received2 = <fish><type>cod</type><side/></fish>

      (expected2 =~ received2).isEqual shouldEqual false

    }

    it("should find equality when the right contains the left example") {

      val expected = <ns:fish battered="true"><type sustainable="false">cod</type><side>chips</side></ns:fish>
      val received = <ns:fish battered="true"><type sustainable="false" oceananic="true">cod</type><side>chips</side><sauce>ketchup</sauce></ns:fish>

      check(expected =~ received)

    }

    it("should not find equality when namespaces do not match") {

      // Note, the <sid> tag *is* equal since the left has less information than the right.
      val expected = <ns:fish><type>haddock</type><side/></ns:fish>
      val received = <fish><type>haddock</type><side>chips</side></fish>

      (expected =~ received).isEqual shouldEqual false

    }

  }

  describe("testing the equality of xml objects with matching rules") {

    it("should be able to accept equality with a rule in a simple case 1") {
      val expected = <fish><type>haddock</type></fish>
      val received = <fish><type>cod</type></fish>

      val rules: Option[Map[String, MatchingRule]] = Option {
        Map(
          "fish" -> MatchingRule(Option("regex"), Some("haddock|cod"), None)
        )
      }

      check(expected =~ received)
    }

    it("should be able to accept equality with a rule in a simple case with a sub element") {
      val expected = <fish><type>haddock</type><side>peas</side></fish>
      val received = <fish><type>haddock</type><side>chips</side></fish>

      val rules: Option[Map[String, MatchingRule]] = Option {
        Map(
          "fish.side" -> MatchingRule(Option("type"), None, None)
        )
      }

      check(expected =~ received)
    }

    it("should be able to handle a more complicated match") {

      val rules: Option[Map[String, MatchingRule]] = Option {
        Map(
          "fish.breed" -> MatchingRule(Option("type"), None, None),
          "fish.side" -> MatchingRule(Option("regex"), Some("peas|chips"), None)
        )
      }

      val expected = <fish><breed>haddock</breed><side>peas</side></fish>
      val received = <fish><breed>cod</breed><side>chips</side></fish>
      check(expected =~ received)

      val expected2 = <fish><breed>haddock</breed><side>peas</side></fish>
      val received2 = <fish><breed>1</breed><side>chips</side></fish>
      (expected2 =~ received2).isEqual shouldEqual false

      val expected3 = <fish><breed>haddock</breed><side>peas</side></fish>
      val received3 = <fish><breed>cod</breed><side>hamburgers</side></fish>
      (expected3 =~ received3).isEqual shouldEqual false

      val expected4 = <fish><breed>haddock</breed><side>peas</side><sauce>ketchup</sauce></fish>
      val received4 = <fish><breed>cod</breed><side>chips</side><sauce>ketchup</sauce></fish>
      check(expected4 =~ received4)

      // Not sure if this is the desired behaviour.
      val expected5 = <fish><breed>haddock</breed><side>peas</side><sauce>ketchup</sauce></fish>
      val received5 = <fish><breed>cod</breed><side>chips</side><sauce>brown</sauce></fish>
      check(expected5 =~ received5)
    }

  }

//  describe("map in map") {
//
//    it("should be able to tell if one map of strings exists in another") {
//
//      SharedXmlEqualityHelpers.mapContainsMap(Map("a" -> "b"), Map("a" -> "b")) shouldEqual true
//      SharedXmlEqualityHelpers.mapContainsMap(Map.empty[String, String], Map("a" -> "b")) shouldEqual true
//      SharedXmlEqualityHelpers.mapContainsMap(Map("a" -> "b", "c" -> "d"), Map("a" -> "b")) shouldEqual false
//      SharedXmlEqualityHelpers.mapContainsMap(Map("a" -> "b"), Map("a" -> "b", "c" -> "d")) shouldEqual true
//      SharedXmlEqualityHelpers.mapContainsMap(Map("a" -> "b"), Map.empty[String, String]) shouldEqual false
//
//    }
//
//  }

}
