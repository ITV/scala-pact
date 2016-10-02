package com.itv.scalapactcore.common

import argonaut.Json

import ColourOuput._

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

        case h::Nil =>
          println("Unexpected next token during matching: " + h)
          rec(Nil, acc :+ RuleMatchFailure)

        case allArrayElements::allFields::remaining if allArrayElements == "[*]" && allFields == "*" =>
          println("Got 4: " + allArrayElements + " - " + allFields)
          rec(remaining, acc :+ checkAllFieldsInAllArrayElements(ruleAndContext, expectedArray, receivedArray))

        case h::t =>
          println("Got 3: " + h)
          rec(t, acc :+ RuleMatchFailure)
      }

    }

    rec(pathSegments, Nil)
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
