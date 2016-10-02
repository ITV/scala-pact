package com.itv.scalapactcore.common

import argonaut.Json

import ColourOuput._

import scalaz._
import Scalaz._
import com.itv.scalapactcore.MatchingRule

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
          val res = listArrayMatchStatusToSingle(acc)

          println(res)

          res

        case h::Nil if h == "[*]" && ruleAndContext.rule.`match`.exists(_ == "type") =>
          println("Got 1: " + h)
          rec(Nil, acc :+ checkAllSimpleValuesInArray(ruleAndContext, expectedArray, receivedArray))

        case h::Nil if h == "*" =>
          println("Got 2: " + h)
          rec(Nil, acc :+ RuleMatchFailure)


        case h::Nil if h.matches("^[A-Za-z0-9-_]+") =>
          println("Got 6: " + h)

          val next = extractSubArrays(h, ruleAndContext, Nil, expectedArray, receivedArray)

          val res = next.receivedArrays.map { ra =>
            //TODO: Missing regex!!
            MatchingRule.unapply(next.ruleAndContext.rule).map {
              case (None, None, Some(arrayMin)) =>
                if (ra.length >= arrayMin) RuleMatchSuccess
                else RuleMatchFailure

              case (Some(matchType), None, Some(arrayMin)) if matchType == "type" =>
                if (ra.length >= arrayMin) RuleMatchSuccess
                else RuleMatchFailure

              case (Some(matchType), None, None) if matchType == "type" =>
                RuleMatchSuccess

              case _ =>
                NoRuleMatchRequired
            }.getOrElse {
              println("Failed to extract rule, failing.".yellow)
              RuleMatchFailure
            }
          }

          rec(Nil, acc ++ res)

        case h::Nil =>
          println("Unexpected next token during matching: " + h)
          rec(Nil, acc :+ RuleMatchFailure)

        case allArrayElements::allFields::remaining if allArrayElements == "[*]" && allFields == "*" =>
          println("Got 4: " + allArrayElements + " - " + allFields)
          rec(remaining, acc :+ checkAllFieldsInAllArrayElements(ruleAndContext, expectedArray, receivedArray))

        case allArrayElements::newArrayToMatch::remaining if allArrayElements == "[*]" && newArrayToMatch.matches("^[A-Za-z0-9-_]+\\[\\*\\]") =>
          println("Got 5: " + allArrayElements + " - " + newArrayToMatch)

          val next = extractSubArrays(newArrayToMatch, ruleAndContext, remaining, expectedArray, receivedArray)

          val res = next.receivedArrays.map { ra =>
            arrayRuleMatchWithWildcards(next.arrayName)(next.ruleAndContext)(next.expectedArray)(ra)
          }

          rec(Nil, acc ++ res)

        case h::t =>
          println("Got 3: " + h)
          rec(t, acc :+ RuleMatchFailure)
      }

    }

    rec(pathSegments, Nil)
  }

  case class NextArrayToMatch(arrayName: String, ruleAndContext: MatchingRuleContext, expectedArray: Json.JsonArray, receivedArrays: List[Json.JsonArray])

  def extractSubArrays(arrayNameToExtract: String, ruleAndContext: MatchingRuleContext, remaining: List[String], expectedArray: Json.JsonArray, receivedArray: Json.JsonArray): NextArrayToMatch = {
    val arrayName = arrayNameToExtract.replace("[*]", "")
    val nextRuleAndContext = ruleAndContext.copy(path = arrayNameToExtract + "." + remaining.mkString("."))
    //          println("> " + arrayName)
    //          println("> " + nextRuleAndContext)
    //          println("> " + expectedArray.headOption.flatMap(_.objectFields))

    val maybeArrayField = expectedArray.headOption.flatMap(_.objectFields.flatMap(_.find(f => f.toString == arrayName)))
    val maybeExpectedArray = (expectedArray.headOption |@| maybeArrayField) { (element, field) => element.field(field) }
    val maybeReceivedArrays = maybeArrayField.map { field =>
      receivedArray.map { a =>
        a.field(field)
      }
    }.getOrElse(Nil)

    val extractedExpectedArray = maybeExpectedArray.flatten.map(_.arrayOrEmpty).getOrElse(Nil)
    val allReceivedArrays = maybeReceivedArrays.map(_.map(_.arrayOrEmpty).getOrElse(Nil))
    //          println("> ex: " + extractedExpectedArray)
    //          println("> rc: " + allReceivedArrays)
    NextArrayToMatch(arrayName, nextRuleAndContext, extractedExpectedArray, allReceivedArrays)
  }

  def checkAllFieldsInAllArrayElements(ruleAndContext: MatchingRuleContext, expectedArray: Json.JsonArray, receivedArray: Json.JsonArray): ArrayMatchingStatus =
    ruleAndContext.rule.`match` match {
      case Some(r) if r == "type" =>
        expectedArray
          .headOption
          .map(h => (h.objectFieldsOrEmpty, h))
          .map { case (fields, ex) =>
            fields.isEmpty || fields.forall { f =>
              ex.field(f) match {
                case Some(x) if x.isString => receivedArray.forall(r => r.field(f).exists(rf => rf.isString))
                case Some(x) if x.isArray => receivedArray.forall(r => r.field(f).exists(rf => rf.isArray))
                case Some(x) if x.isBool => receivedArray.forall(r => r.field(f).exists(rf => rf.isBool))
                case Some(x) if x.isNull => receivedArray.forall(r => r.field(f).exists(rf => rf.isNull))
                case Some(x) if x.isNumber => receivedArray.forall(r => r.field(f).exists(rf => rf.isNumber))
                case Some(x) if x.isObject => receivedArray.forall(r => r.field(f).exists(rf => rf.isObject))
                case None =>
                  println("Somehow failed to get expected field " + f)
                  false
              }
            }
          } match {
          case Some(b) if b => RuleMatchSuccess
          case Some(b) => RuleMatchFailure
          case None => RuleMatchFailure
        }

      case Some(r) if r == "regex" =>
        // Bit of a weird one. We're saying that all field values in all elements must match this here regex :-S
        val bool = receivedArray.forall { p =>
          p.isObject &&
          p.objectFieldsOrEmpty.forall { f =>
            p.field(f)
              .flatMap(_.string)
              .exists(_.matches(ruleAndContext.rule.regex.getOrElse(".")))
          }
        }

        if(bool) RuleMatchSuccess else RuleMatchFailure

      case t =>
        println(("Unknown test type: " + t).yellow)
        RuleMatchFailure
    }

  def checkAllSimpleValuesInArray(ruleAndContext: MatchingRuleContext, expectedArray: Json.JsonArray, receivedArray: Json.JsonArray): ArrayMatchingStatus =
    ruleAndContext.rule.`match` match {
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

}
