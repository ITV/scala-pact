package com.itv.scalapactcore.common.matching

import argonaut._
import com.itv.scalapactcore.MatchingRule
import BodyMatching.BodyMatchingRules

import scala.language.implicitConversions
import scalaz.Scalaz._

import com.itv.scalapactcore.common.ColourOuput._

object ScalaPactJsonEquality {

  implicit def toJsonEqualityWrapper(json: Json): JsonEqualityWrapper = JsonEqualityWrapper(json)

  case class JsonEqualityWrapper(json: Json) {
    def =~(to: Json): BodyMatchingRules => Boolean = matchingRules =>
      PermissiveJsonEqualityHelper.areEqual(
        matchingRules.map(p => p.filter(r => r._1.startsWith("$.body"))),
        json,
        to,
        ""
      )

    def =<>=(to: Json): Boolean => BodyMatchingRules => Boolean = beSelectivelyPermissive => matchingRules =>
      StrictJsonEqualityHelper.areEqual(
        beSelectivelyPermissive,
        matchingRules.map(p => p.filter(r => r._1.startsWith("$.body"))),
        json,
        to,
        ""
      )
  }

}

object StrictJsonEqualityHelper {

  def areEqual(beSelectivelyPermissive: Boolean, matchingRules: BodyMatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean =
    expected match {
      case j: Json if j.isObject && received.isObject =>
        compareFields(beSelectivelyPermissive, matchingRules, expected, received, j.objectFieldsOrEmpty, accumulatedJsonPath)

      case j: Json if j.isArray && received.isArray =>
        compareArrays(beSelectivelyPermissive, matchingRules, j.array, received.array, accumulatedJsonPath)

      case j: Json =>
        SharedJsonEqualityHelpers.compareValues(matchingRules, expected, received, accumulatedJsonPath)
    }

  private def compareArrays(beSelectivelyPermissive: Boolean, matchingRules: BodyMatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): Boolean = {
    def compareElements: Boolean = {
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

    SharedJsonEqualityHelpers.matchArrayWithRules(matchingRules, expectedArray, receivedArray, accumulatedJsonPath) match {
      case RuleMatchSuccess => true
      case RuleMatchFailure => false
      case NoRuleMatchRequired => compareElements
    }
  }

  private def compareFields(beSelectivelyPermissive: Boolean, matchingRules: BodyMatchingRules, expected: Json, received: Json, expectedFields: List[Json.JsonField], accumulatedJsonPath: String): Boolean = {
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

object PermissiveJsonEqualityHelper {

  /***
    * Permissive equality means that the elements and fields defined in the 'expected'
    * are required to be present in the 'received', however, extra elements on the right
    * are allowed and ignored. Additionally elements are still considered equal if their
    * fields or array elements are out of order, as long as they are present since json
    * doesn't not guarantee element order.
    */
  def areEqual(matchingRules: BodyMatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean =
    expected match {
      case j: Json if j.isObject && received.isObject =>
        compareObjects(matchingRules, expected, received, j.objectFieldsOrEmpty, accumulatedJsonPath)

      case j: Json if j.isArray && received.isArray =>
        compareArrays(matchingRules, j.array, received.array, accumulatedJsonPath)

      case j: Json =>
        SharedJsonEqualityHelpers.compareValues(matchingRules, expected, received, accumulatedJsonPath)
    }

  private def compareArrays(matchingRules: BodyMatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): Boolean = {
    def compareElements: Boolean = {
      (expectedArray |@| receivedArray) { (ja, ra) =>
        ja.zipWithIndex.forall { case (jo, i) =>
          ra.exists(ro => areEqual(matchingRules, jo, ro, accumulatedJsonPath + s"[$i]"))
        }
      } match {
        case Some(matches) => matches
        case None => false
      }
    }

    SharedJsonEqualityHelpers.matchArrayWithRules(matchingRules, expectedArray, receivedArray, accumulatedJsonPath) match {
      case RuleMatchSuccess => true
      case RuleMatchFailure => false
      case NoRuleMatchRequired => compareElements
    }
  }


  private def compareObjects(matchingRules: BodyMatchingRules, expected: Json, received: Json, expectedFields: List[Json.JsonField], accumulatedJsonPath: String): Boolean =
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

object SharedJsonEqualityHelpers {

  val findMatchingRules: String => Map[String, MatchingRule] => Option[List[MatchingRuleContext]] = accumulatedJsonPath => m =>
    if (accumulatedJsonPath.length > 0) {
      m.map(r => (r._1.replace("['", ".").replace("']", ""), r._2)).filter { r =>
        r._1.endsWith(accumulatedJsonPath) || WildCardRuleMatching.findMatchingRuleWithWildCards(accumulatedJsonPath)(r._1)
      }.map(kvp => MatchingRuleContext(kvp._1.replace("$.body", ""), kvp._2)).toList.some
    } else None

  def compareValues(matchingRules: BodyMatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean =
    (matchingRules >>= findMatchingRules(accumulatedJsonPath)).map(_.map(_.rule)) match {
      case Some(rules) if rules.nonEmpty =>
        rules.forall {
          case rule if rule.`match`.exists(_ == "type") => //Use exists for 2.10 compat
            expected.name == received.name

          case rule if received.isString && rule.`match`.exists(_ == "regex") && rule.regex.isDefined => //Use exists for 2.10 compat
            rule.regex.exists { regexRule =>
              received.string.exists(_.matches(regexRule))
            }

          case rule =>
            println(("Found unknown rule '" + rule + "' for path '" + accumulatedJsonPath + "' while matching " + expected.toString + " with " + received.toString()).yellow)
            false
        }

      case Some(rules) if rules.isEmpty =>
        expected == received

      case None =>
        expected == received
    }

  def matchArrayWithRules(matchingRules: BodyMatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): ArrayMatchingStatus = {

    def checkRule(currentPath: String, ruleAndContext: MatchingRuleContext, ea: Json.JsonArray, ra: Json.JsonArray): ArrayMatchingStatus = {

      //TODO: Missing regex...?
      if(currentPath == ruleAndContext.path) {
        MatchingRule.unapply(ruleAndContext.rule).map {
          case (None, None, Some(arrayMin)) =>
            if (ra.length >= arrayMin) RuleMatchSuccess
            else RuleMatchFailure

          case (Some(matchType), None, Some(arrayMin)) if matchType == "type" =>
            // Yay typed languages! We know the types are equal, they're both arrays!
            if (ra.length >= arrayMin) RuleMatchSuccess
            else RuleMatchFailure

          case (Some(matchType), None, None) if matchType == "type" =>
            // Yay typed languages! We know the types are equal, they're both arrays!
            RuleMatchSuccess

          case _ =>
            NoRuleMatchRequired
        }.getOrElse(NoRuleMatchRequired)
      } else if(ruleAndContext.path.contains("*")) {
        // We have a rule that isn't a simple match on the path and includes a wildcard.
        WildCardRuleMatching.arrayRuleMatchWithWildcards(currentPath)(ruleAndContext)(ea)(ra)
      } else if(ruleAndContext.path.startsWith(currentPath + "[")) {
        // We have a rule that isn't a simple match on the path and may be an array positional match like fish[2]
        WildCardRuleMatching.arrayRuleMatchWithWildcards(currentPath)(ruleAndContext)(ea)(ra)
      } else {
        println(("Unknown rule type: '" + ruleAndContext.rule + "' for path '" + ruleAndContext.path).yellow)
        RuleMatchFailure
      }
    }

    (expectedArray |@| receivedArray) { (ja, ra) =>
      matchingRules >>= findMatchingRules(accumulatedJsonPath) match {

        case Some(rules) =>
          ArrayMatchingStatus.listArrayMatchStatusToSingle(rules.map(r => checkRule(accumulatedJsonPath, r, ja, ra)))

        case None =>
          NoRuleMatchRequired

      }
    }.getOrElse(NoRuleMatchRequired)
  }

}
