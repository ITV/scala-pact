package com.itv.scalapactcore.common

import scala.language.implicitConversions
import argonaut._
import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.InteractionMatchers.MatchingRules

import scalaz._
import Scalaz._

import ColourOuput._

object ScalaPactJsonEquality {

  implicit def toJsonEqualityWrapper(json: Json): JsonEqualityWrapper = JsonEqualityWrapper(json)

  case class JsonEqualityWrapper(json: Json) {
    def =~(to: Json): MatchingRules => Boolean = matchingRules =>
      PermissiveJsonEqualityHelper.areEqual(
        matchingRules.map(p => p.filter(r => r._1.startsWith("$.body"))),
        json,
        to,
        ""
      )

    def =<>=(to: Json): Boolean => MatchingRules => Boolean = beSelectivelyPermissive => matchingRules =>
      StrictJsonEqualityHelper.areEqual(
        beSelectivelyPermissive,
        matchingRules.map(p => p.filter(r => r._1.startsWith("$.body"))),
        json,
        to,
        ""
      )
  }

}

object StrictJsonEqualityHelper extends SharedJsonEqualityHelpers {

  def areEqual(beSelectivelyPermissive: Boolean, matchingRules: MatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean = {
    expected match {
      case j: Json if j.isObject && received.isObject =>
        compareFields(beSelectivelyPermissive, matchingRules, expected, received, j.objectFieldsOrEmpty, accumulatedJsonPath)

      case j: Json if j.isArray && received.isArray =>
        compareArrays(beSelectivelyPermissive, matchingRules, j.array, received.array, accumulatedJsonPath)

      case j: Json =>
        compareValues(matchingRules, expected, received, accumulatedJsonPath)
    }
  }

  private def compareArrays(beSelectivelyPermissive: Boolean, matchingRules: MatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): Boolean = {
    (expectedArray |@| receivedArray) { (ja, ra) =>
      if (ja.length == ra.length) {
        ja.zip(ra).zipWithIndex.forall { case (pair, i) =>
          areEqual(beSelectivelyPermissive, matchingRules, pair._1, pair._2, accumulatedJsonPath + s"[$i]")
        }
      } else {
        false
      }
    } match {
      case Some(matches) => matches
      case None => false
    }
  }

  private def compareFields(beSelectivelyPermissive: Boolean, matchingRules: MatchingRules, expected: Json, received: Json, expectedFields: List[Json.JsonField], accumulatedJsonPath: String): Boolean = {
    if (!expectedFields.forall(f => received.hasField(f))) false
    else {
      if(beSelectivelyPermissive) {
        expectedFields.forall { field =>

          (expected.field(field) |@| received.field(field)){ areEqual(beSelectivelyPermissive, matchingRules, _, _, accumulatedJsonPath + s".${field.toString}") } match {
            case Some(bool) => bool
            case None => false
          }
        }
      } else {
        if (expected.objectFieldsOrEmpty.length == received.objectFieldsOrEmpty.length) {
          expectedFields.forall { field =>
            (expected.field(field) |@| received.field(field)) { (e, r) =>
              areEqual(beSelectivelyPermissive, matchingRules, e, r, accumulatedJsonPath + s".${field.toString}")
            } match {
              case Some(bool) => bool
              case None => false
            }
          }
        } else {
          false
        }
      }
    }
  }

}

object PermissiveJsonEqualityHelper extends SharedJsonEqualityHelpers {

  /***
    * Permissive equality means that the elements and fields defined in the 'expected'
    * are required to be present in the 'received', however, extra elements on the right
    * are allowed and ignored. Additionally elements are still considered equal if their
    * fields or array elements are out of order, as long as they are present since json
    * doesn't not guarantee element order.
    */
  def areEqual(matchingRules: MatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean = {
    expected match {
      case j: Json if j.isObject && received.isObject =>
        compareFields(matchingRules, expected, received, j.objectFieldsOrEmpty, accumulatedJsonPath)

      case j: Json if j.isArray && received.isArray =>
        compareArrays(matchingRules, j.array, received.array, accumulatedJsonPath)

      case j: Json =>
        compareValues(matchingRules, expected, received, accumulatedJsonPath)
    }
  }

  private def compareArrays(matchingRules: MatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): Boolean =
    (expectedArray |@| receivedArray) { (ja, ra) =>
      ja.zipWithIndex.forall { case (jo, i) =>
        ra.exists(ro => areEqual(matchingRules, jo, ro, accumulatedJsonPath + s"[$i]"))
      }
    } match {
      case Some(matches) => matches
      case None => false
    }


  private def compareFields(matchingRules: MatchingRules, expected: Json, received: Json, expectedFields: List[Json.JsonField], accumulatedJsonPath: String): Boolean =
    if(!expectedFields.forall(f => received.hasField(f))) false
    else {
      expectedFields.forall { field =>
        (expected.field(field) |@| received.field(field)){ (e, r) => areEqual(matchingRules, e, r, accumulatedJsonPath + s".${field.toString}") } match {
          case Some(bool) => bool
          case None => false
        }
      }
    }

}

sealed trait SharedJsonEqualityHelpers {

  protected def compareValues(matchingRules: MatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean = {

    val findMatchingRule = (m: Map[String, MatchingRule]) => m.map(r => (r._1.replace("['", ".").replace("']", ""), r._2)).find { r =>
      accumulatedJsonPath.length > 0 && r._1.endsWith(accumulatedJsonPath)
    }.map(_._2)

    matchingRules >>= findMatchingRule match {
      case Some(rule) if rule.`match` == "type" =>
        expected.name == received.name

      case Some(rule) if received.isString && rule.`match` == "regex" && rule.regex.isDefined =>
        rule.regex.exists { regexRule =>
          received.string.exists(_.matches(regexRule))
        }

      case Some(rule) =>
        println(("Found unknown rule '" + rule + "' for path '" + accumulatedJsonPath + "' while matching " + expected.toString + " with " + received.toString()).yellow)
        false

      case None =>
        expected == received
    }

  }

}
