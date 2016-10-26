package com.itv.scalapactcore.common.matching

import argonaut.Json
import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common._

import ColourOuput._

object WildCardRuleMatching {

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

    val pathSegments = ruleAndContext.copy(path = ruleAndContext.path.replace(currentPath, "")).path.split('.').toList

    def rec(remainingSegments: List[String], acc: List[ArrayMatchingStatus], ea: Json.JsonArray, ra: Json.JsonArray): ArrayMatchingStatus = {
      remainingSegments match {
        case Nil =>
          ArrayMatchingStatus.listArrayMatchStatusToSingle(acc)

        case h::Nil if h == "[*]" =>
          rec(Nil, acc :+ checkAllSimpleValuesInArray(ruleAndContext, ea, ra), ea, ra)

        case h::Nil if h == "*" =>
          rec(Nil, acc :+ RuleMatchFailure, ea, ra)

        case h::Nil if h.matches("^[A-Za-z0-9-_]+") =>
          val next = extractSubArrays(h, ruleAndContext, Nil, ea, ra)
          val res = checkFieldInAllArrayElements(next)

          rec(Nil, acc ++ res, ea, ra)

        case h::Nil if h.matches("\\[\\d+\\]") =>
          //Specific element in array, reduce ra to that one element, reset remaining and recurse.
          """\d+""".r.findFirstIn(h).flatMap(safeStringToInt).map { index =>
            rec("[*]" :: Nil, acc, ea, List(ra(index)))
          }.getOrElse(RuleMatchFailure)

        case h::Nil =>
          rec(Nil, acc :+ RuleMatchFailure, ea, ra)

        case allArrayElements::allFields::remaining if allArrayElements == "[*]" && allFields == "*" =>
          rec(remaining, acc :+ checkAllFieldsInAllArrayElements(ruleAndContext, ea, ra), ea, ra)

        case allArrayElements::newArrayToMatch::remaining if allArrayElements == "[*]" && newArrayToMatch.matches("^[A-Za-z0-9-_]+\\[\\*\\]") =>
          val next = extractSubArrays(newArrayToMatch, ruleAndContext, remaining, ea, ra)

          val res = next.received.map(_.arrayOrEmpty).map { ra =>
            arrayRuleMatchWithWildcards(next.fieldName)(next.ruleAndContext)(next.expected.arrayOrEmpty)(ra)
          }

          rec(Nil, acc ++ res, ea, ra)

        case allArrayElements::oneField::_ if allArrayElements == "[*]" && oneField.matches("^[A-Za-z0-9-_]+") =>
          val next = extractSubArrays(oneField, ruleAndContext, Nil, ea, ra)
          val res = checkFieldInAllArrayElements(next)

          rec(Nil, acc ++ res, ea, ra)

        case _::t =>
          rec(t, acc :+ RuleMatchFailure, ea, ra)
      }

    }

    rec(pathSegments, Nil, expectedArray, receivedArray)
  }

  private lazy val safeStringToInt: String => Option[Int] = str => try { Option(str.toInt) } catch { case e: Throwable => None }

  private case class NextArrayToMatch(fieldName: String, ruleAndContext: MatchingRuleContext, expected: Json, received: List[Json])

  private def extractSubArrays(arrayNameToExtract: String, ruleAndContext: MatchingRuleContext, remaining: List[String], expectedArray: Json.JsonArray, receivedArray: Json.JsonArray): NextArrayToMatch = {
    val arrayName = arrayNameToExtract.replace("[*]", "")
    val nextRuleAndContext = ruleAndContext.copy(path = (arrayNameToExtract :: remaining).mkString("."))

    val maybeArrayField = expectedArray.headOption.flatMap(_.objectFields.flatMap(_.find(f => f.toString == arrayName)))

    val maybeExpectedArray =
      expectedArray.headOption.flatMap { element =>
        maybeArrayField.flatMap { field =>
          element.field(field)
        }
      }

    val maybeReceivedArrays = maybeArrayField.map { field =>
      receivedArray.map { a =>
        a.field(field)
      }
    }.getOrElse(Nil)

    val extractedExpectedArray = maybeExpectedArray.getOrElse(Json.jEmptyObject)
    val allReceivedArrays = maybeReceivedArrays

    NextArrayToMatch(arrayName, nextRuleAndContext, extractedExpectedArray, allReceivedArrays.map(_.getOrElse(Json.jEmptyObject)))
  }

  private def checkFieldInAllArrayElements(next: NextArrayToMatch): List[ArrayMatchingStatus] =
    next.received.map { ra =>
      //TODO: Missing regex!!
      MatchingRule.unapply(next.ruleAndContext.rule).map {
        case (None, None, Some(arrayMin)) =>
          if (ra.isArray && ra.arrayOrEmpty.length >= arrayMin) RuleMatchSuccess
          else RuleMatchFailure

        case (Some(matchType), None, Some(arrayMin)) if matchType == "type" =>
          if (ra.isArray && ra.arrayOrEmpty.length >= arrayMin) RuleMatchSuccess
          else RuleMatchFailure

        case (Some(matchType), None, None) if matchType == "type" =>
          RuleMatchSuccess

        case (Some(matchType), None, None) if matchType == "regex" =>
          println("Regex match specified but no rule was supplied".yellow)
          RuleMatchFailure

        case (Some(matchType), Some(regularExpression), None) if matchType == "regex" =>
          if(ra.isString && ra.string.exists(_.matches(regularExpression))) RuleMatchSuccess
          else RuleMatchFailure

        case (None, None, None) =>
          println("Rule match ignored since no rule was available.".yellow)
          NoRuleMatchRequired

      }.getOrElse {
        println("Failed to extract rule, failing.".yellow)
        RuleMatchFailure
      }
    }

  private def checkAllFieldsInAllArrayElements(ruleAndContext: MatchingRuleContext, expectedArray: Json.JsonArray, receivedArray: Json.JsonArray): ArrayMatchingStatus =
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

  private def checkAllSimpleValuesInArray(ruleAndContext: MatchingRuleContext, expectedArray: Json.JsonArray, receivedArray: Json.JsonArray): ArrayMatchingStatus =
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
          p.isString && p.string.exists(_.matches(ruleAndContext.rule.regex.getOrElse(".")))
        }

        if(bool) RuleMatchSuccess else RuleMatchFailure

      case t =>
        println(("Unknown test type: " + t).yellow)
        RuleMatchFailure
    }

}
