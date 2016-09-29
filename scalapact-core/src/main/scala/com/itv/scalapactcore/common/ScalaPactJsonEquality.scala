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

    println(">S:  " + accumulatedJsonPath)

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

    matchArrayWithRules(matchingRules, expectedArray, receivedArray, accumulatedJsonPath) match {
      case RuleMatchSuccess => true
      case RuleMatchFailure => false
      case NoRuleMatchRequired => compareElements
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

    println(">P:  " + accumulatedJsonPath)

    expected match {
      case j: Json if j.isObject && received.isObject =>
        compareObjects(matchingRules, expected, received, j.objectFieldsOrEmpty, accumulatedJsonPath)

      case j: Json if j.isArray && received.isArray =>
        compareArrays(matchingRules, j.array, received.array, accumulatedJsonPath)

      case j: Json =>
        compareValues(matchingRules, expected, received, accumulatedJsonPath)
    }
  }

  private def compareArrays(matchingRules: MatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): Boolean = {
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

    matchArrayWithRules(matchingRules, expectedArray, receivedArray, accumulatedJsonPath) match {
      case RuleMatchSuccess => true
      case RuleMatchFailure => false
      case NoRuleMatchRequired => compareElements
    }
  }


  private def compareObjects(matchingRules: MatchingRules, expected: Json, received: Json, expectedFields: List[Json.JsonField], accumulatedJsonPath: String): Boolean =
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

  protected val findMatchingRules: String => Map[String, MatchingRule] => Option[List[MatchingRuleContext]] = accumulatedJsonPath => m => {
//    println(accumulatedJsonPath)
//    println(m)
    if (accumulatedJsonPath.length > 0) {
      m.map(r => (r._1.replace("['", ".").replace("']", ""), r._2)).filter { r =>
        r._1.endsWith(accumulatedJsonPath) || WildCardRuleMatching.findMatchingRuleWithWildCards(accumulatedJsonPath)(r._1)
      }.map(kvp => MatchingRuleContext(kvp._1.replace("$.body", ""), kvp._2)).toList.some
    } else None
  }

  protected def compareValues(matchingRules: MatchingRules, expected: Json, received: Json, accumulatedJsonPath: String): Boolean =
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

  protected def matchArrayWithRules(matchingRules: MatchingRules, expectedArray: Option[Json.JsonArray], receivedArray: Option[Json.JsonArray], accumulatedJsonPath: String): ArrayMatchingStatus = {

    def checkRule(currentPath: String, ruleAndContext: MatchingRuleContext, ea: Json.JsonArray, ra: Json.JsonArray): ArrayMatchingStatus = {

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
      } else {
        println(("Unknown rule type: '" + ruleAndContext.rule + "' for path '" + ruleAndContext.path).yellow)
        RuleMatchFailure
      }
    }

    (expectedArray |@| receivedArray) { (ja, ra) =>
      matchingRules >>= findMatchingRules(accumulatedJsonPath) match {

        case Some(rules) =>
          println("Rules:\n - " + rules.mkString("\n - "))

          WildCardRuleMatching.listArrayMatchStatusToSingle(rules.map(r => checkRule(accumulatedJsonPath, r, ja, ra)))

        case None =>
          NoRuleMatchRequired

      }
    }.getOrElse(NoRuleMatchRequired)
  }

}

sealed trait ArrayMatchingStatus
case object RuleMatchSuccess extends ArrayMatchingStatus
case object RuleMatchFailure extends ArrayMatchingStatus
case object NoRuleMatchRequired extends ArrayMatchingStatus

case class MatchingRuleContext(path: String, rule: MatchingRule)

object WildCardRuleMatching {

  val listArrayMatchStatusToSingle: List[ArrayMatchingStatus] => ArrayMatchingStatus = {
    case l: List[ArrayMatchingStatus] if l.contains(RuleMatchFailure) => RuleMatchFailure
    case l: List[ArrayMatchingStatus] if l.contains(RuleMatchSuccess) => RuleMatchSuccess
    case _ => NoRuleMatchRequired
  }

  val findMatchingRuleWithWildCards: String => String => Boolean = accumulatedJsonPath => rulePath => {
    val regexMatch = accumulatedJsonPath.matches(
      rulePath
        .replace("$.body", "")
        .replace("[*]", "\\[\\d+\\]")
        .replace(".*", "\\.[A-Za-z0-9-_]+")
    )

    val containsPathToArray = rulePath.replace("$.body", "").replaceAll("\\[\\*\\]", "").contains(accumulatedJsonPath.replaceAll("\\[\\d+\\]", ""))

    regexMatch || containsPathToArray
  }

  val arrayRuleMatchWithWildcards: String => MatchingRuleContext => Json.JsonArray => Json.JsonArray => ArrayMatchingStatus = currentPath => ruleAndContext => expectedArray => receivedArray => {
    println("----")
    println(currentPath + " : " + ruleAndContext)
    println(expectedArray)
    println(receivedArray)

    val pathSegments = ruleAndContext.copy(path = ruleAndContext.path.replace(currentPath, "")).path.split('.').toList

    println(pathSegments)

    def rec(remainingSegments: List[String], acc: List[ArrayMatchingStatus]): ArrayMatchingStatus = {
      remainingSegments match {
        case Nil =>
          listArrayMatchStatusToSingle(acc)

        case h::Nil if h == "[*]" && ruleAndContext.rule.`match`.exists(_ == "type") =>
          println("Got 1: " + h)

          val checkAll = ruleAndContext.rule.`match` match {
            case Some(r) if r == "type" =>
                expectedArray.headOption.map {
                  case x if x.isString => receivedArray.forall(_.isString)
                  case x if x.isArray => receivedArray.forall(_.isArray)
                  case x if x.isBool => receivedArray.forall(_.isBool)
                  case x if x.isNull => receivedArray.forall(_.isNull)
                  case x if x.isNumber => receivedArray.forall(_.isNumber)
                  case x if x.isObject => receivedArray.forall(_.isObject)
                }
                .map(b => if(b) RuleMatchSuccess else RuleMatchFailure)
                .getOrElse {
                  println("Required type check but gave no example to derive type from.".yellow)
                  RuleMatchFailure
                }

            case Some(r) if r == "regex" =>
              val bool = receivedArray.forall { p =>
                p.isString && p.string.exists(s => s.matches(ruleAndContext.rule.regex.getOrElse(".")))
              }

              if(bool) RuleMatchSuccess else RuleMatchFailure

            case t =>
              println(("Unknown test type: " + t).yellow)
              RuleMatchFailure
          }


          rec(Nil, List(checkAll))

        case h::Nil =>
          println("Unexpected next token during matching: " + h)
          rec(Nil, List(RuleMatchFailure))

        case h::t =>
          println("Got 3: " + h)
          rec(t, List(RuleMatchFailure))
      }

    }

    rec(pathSegments, Nil)
  }

}