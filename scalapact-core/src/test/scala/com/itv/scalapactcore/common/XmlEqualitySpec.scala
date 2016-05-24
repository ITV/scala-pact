package com.itv.scalapactcore.common

import org.scalatest.{FunSpec, Matchers}

import PermissiveXmlEquality._

class XmlEqualitySpec extends FunSpec with Matchers {

  describe("testing the equality of xml objects") {

//    it("should find equality of a simple example") {
//
//      val expected = <fish><type>cod</type><side/></fish>
//      val received = <fish><type>cod</type><side/></fish>
//
//      expected =~ received shouldEqual true
//
//    }
//
//    it("should not find equality of a simple unequal example") {
//
//      val expected = <fish><type>cod</type><side/></fish>
//      val received = <fish><type>cod</type><side>chips</side></fish>
//
//      expected =~ received shouldEqual false
//
//    }

    it("should find equality when the right contains the left example") {

      val expected = <fish battered="true"><type sustainable="false">cod</type><side>chips</side></fish>
      val received = <fish battered="true"><type sustainable="false" oceananic="true">cod</type><side>chips</side><sauce>ketchup</sauce></fish>

      expected =~ received shouldEqual true

    }

  }

  describe("map in map") {

    it("should be able to tell if one map of strings exists in another") {

      PermissiveXmlEqualityHelper.mapContainsMap(Map("a" -> "b"))(Map("a" -> "b")) shouldEqual true
      PermissiveXmlEqualityHelper.mapContainsMap(Map.empty[String, String])(Map("a" -> "b")) shouldEqual true
      PermissiveXmlEqualityHelper.mapContainsMap(Map("a" -> "b", "c" -> "d"))(Map("a" -> "b")) shouldEqual false
      PermissiveXmlEqualityHelper.mapContainsMap(Map("a" -> "b"))(Map("a" -> "b", "c" -> "d")) shouldEqual true
      PermissiveXmlEqualityHelper.mapContainsMap(Map("a" -> "b"))(Map.empty[String, String]) shouldEqual false

    }

  }

}
