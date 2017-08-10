package com.itv.scalapactcore.common.matchir

import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.Helpers
import com.itv.scalapactcore.common.matching._

import scala.language.implicitConversions

object IrEquality {

//  implicit def toIrEqualityWrapper(node: IrNode): IrEqualityWrapper = IrEqualityWrapper(node)
//
//  case class IrEqualityWrapper(node: IrNode) {
//    def =~(to: IrNode): Option[Map[String, MatchingRule]] => Boolean = matchingRules =>
//      PermissiveIrEqualityHelper.areEqual(xmlPathToJsonPath(matchingRules), node, to, node.label)
//    def =<>=(to: IrNode): Boolean => Option[Map[String, MatchingRule]] => Boolean = beSelectivelyPermissive => matchingRules =>
//      StrictIrEqualityHelper.areEqual(beSelectivelyPermissive, xmlPathToJsonPath(matchingRules), node, to, node.label)
//  }
//
//  //TODO: Is this a thing? Must be... but when?
//  private def xmlPathToJsonPath(matchingRules: Option[Map[String, MatchingRule]]): Map[String, MatchingRule] =
//    matchingRules.map { mrs =>
//      mrs.map { mr =>
//        (mr._1.replaceAll("""\[\'""", ".").replaceAll("""\'\]""", ""), mr._2)
//      }
//    }.getOrElse(Map.empty[String, MatchingRule])

}
/*
object StrictIrEqualityHelper {

  def areEqual(beSelectivelyPermissive: Boolean, matchingRules: Map[String, MatchingRule], expected: IrNode, received: IrNode, accumulatedXmlPath: String): Boolean =
    compareNodes(beSelectivelyPermissive, matchingRules, expected, received, accumulatedXmlPath)


  def compareNodes(beSelectivelyPermissive: Boolean, matchingRules: Map[String, MatchingRule], expected: IrNode, received: IrNode, accumulatedXmlPath: String): Boolean = {
    SharedIrEqualityHelpers.matchNodeWithRules(matchingRules, accumulatedXmlPath, expected, received) match {
      case RuleMatchSuccess => true
      case RuleMatchFailure => false
      case NoRuleMatchRequired =>
        lazy val prefixEqual = expected.prefix == received.prefix
        lazy val labelEqual = expected.label == received.label
        lazy val attributesLengthOk =
          if(beSelectivelyPermissive) expected.attributes.length <= received.attributes.length
          else expected.attributes.length == received.attributes.length

        lazy val attributesEqual = SharedXmlEqualityHelpers.checkAttributeEquality(matchingRules, accumulatedXmlPath, expected.attributes.asAttrMap, received.attributes.asAttrMap)
        lazy val childLengthOk =
          if(beSelectivelyPermissive) expected.child.length <= received.child.length
          else expected.child.length == received.child.length

        lazy val childrenEqual =
          if(expected.child.isEmpty) expected.text == received.text
          else {
            expected.child.zip(received.child).forall(p => compareNodes(beSelectivelyPermissive, matchingRules, p._1, p._2, accumulatedXmlPath + "." + expected.label))
          }

        prefixEqual && labelEqual && attributesLengthOk && attributesEqual && childLengthOk && childrenEqual
    }
  }

}

object PermissiveIrEqualityHelper {

  /***
    * Permissive equality means that the elements and fields defined in the 'expected'
    * are required to be present in the 'received', however, extra elements on the right
    * are allowed and ignored. Additionally elements are still considered equal if their
    * fields or array elements are out of order, as long as they are present since json
    * doesn't not guarantee element order.
    */
  def areEqual(matchingRules: Map[String, MatchingRule], expected: IrNode, received: IrNode, accumulatedXmlPath: String): Boolean =
    compareNodes(matchingRules, expected, received, accumulatedXmlPath)

  def compareNodes(matchingRules: Map[String, MatchingRule], expected: IrNode, received: IrNode, accumulatedXmlPath: String): Boolean = {
    SharedIrEqualityHelpers.matchNodeWithRules(matchingRules, accumulatedXmlPath, expected, received) match {
      case RuleMatchSuccess => true
      case RuleMatchFailure => false
      case NoRuleMatchRequired =>

        lazy val prefixEqual = expected.prefix == received.prefix
        lazy val labelEqual = expected.label == received.label
        lazy val attributesEqual = SharedXmlEqualityHelpers.checkAttributeEquality(matchingRules, accumulatedXmlPath, expected.attributes.asAttrMap, received.attributes.asAttrMap)
        lazy val childLengthOk = expected.child.length <= received.child.length

        lazy val childrenEqual =
          if(expected.child.isEmpty) expected.text == received.text
          else expected.child.forall { eN => received.child.exists(rN => compareNodes(matchingRules, eN, rN, accumulatedXmlPath + "." + eN.label)) }

        prefixEqual && labelEqual && attributesEqual && childLengthOk && childrenEqual
    }
  }

}

object SharedIrEqualityHelpers {
  // Maybe negative, must have digits, may have decimal and if so must have a
  // digit after it, can have more trailing digits.
  val isNumericValueRegex = """-?\d*\.?\d*$"""
  val isBooleanValueRegex = """true|false"""

  def checkAttributeEquality(matchingRules: Map[String, MatchingRule], accumulatedXmlPath: String, e: Map[String, String], r: Map[String, String]): Boolean = {
    val rulesMap = e.flatMap { ex =>
      Map(ex._1 -> findMatchingRules(accumulatedXmlPath + ".@" + ex._1, matchingRules))
    }.filter(_._2.nonEmpty)

    val (attributesWithRules, attributesWithoutRules) = e.partition(p => rulesMap.contains(p._1))

    if(attributesWithRules.isEmpty){
      SharedXmlEqualityHelpers.mapContainsMap(e, r)
    } else {
      attributesWithRules.forall(a => attributeRulesCheck(rulesMap, r, a) && mapContainsMap(attributesWithoutRules, r))
    }
  }

  def mapContainsMap(e: Map[String, String], r: Map[String, String]): Boolean =
    e.forall { ee =>
      r.exists(rr => rr._1 == ee._1 && rr._2 == ee._2)
    }

  def mapContainsMapIgnoreValues(e: Map[String, String], r: Map[String, String]): Boolean =
    e.forall { ee =>
      r.exists(rr => rr._1 == ee._1)
    }

  private def attributeRulesCheck(rulesMap: Map[String, List[MatchingRuleContext]], receivedAttributes: Map[String, String], attribute: (String, String)): Boolean =
    rulesMap(attribute._1).map(_.rule).forall(attributeRuleTest(receivedAttributes, attribute))

  private def attributeRuleTest(receivedAttributes: Map[String, String], attribute: (String, String))(rule: MatchingRule): Boolean = {
//    val a = MatchingRule.unapply(rule)

    MatchingRule.unapply(rule) match {
      case Some((Some(matchType), None, None)) if matchType == "type" =>
        receivedAttributes
          .get(attribute._1)
          .map { value =>
            // Another best effort type check
            attribute._2 match {
              case x if x.isEmpty => // Empty
                //                println(s"Expect attribute '${attribute._1}' to be empty, and got: $value'".yellow)
                value.isEmpty

              case x if x.matches(isNumericValueRegex) => // Any numeric, can be negative, can have decimal places
                //                println(s"Expect attribute '${attribute._1}' to be numeric, and got: $value'".yellow)
                value.matches(isNumericValueRegex)

              case x if x.matches(isBooleanValueRegex) => // Any Boolean
                //                println(s"Expect attribute '${attribute._1}' to be a boolean, and got: $value'".yellow)
                value.matches(isBooleanValueRegex)

              case x => // Finally, any arbitrary string
                //                println(s"Cannot identify type of attribute '${attribute._1}' assuming string".yellow)
                true
            }
          }
          .exists(_ == true)

      case Some((Some(matchType), Some(regex), _)) if matchType == "regex" =>
        receivedAttributes
          .get(attribute._1)
          .map(_.matches(regex))
          .getOrElse(false)

      case _ =>
        //        println("Unexpected match type during attribute matching with rule".red)
        false

    }
  }

  private def findMatchingRules(accumulatedJsonPath: String, m: Map[String, MatchingRule]): List[MatchingRuleContext] =
    if (accumulatedJsonPath.length > 0) {
      //m.map(
      m.map(r => (r._1.replace("['", ".").replace("']", ""), r._2)).filter { r =>
        r._1.endsWith(accumulatedJsonPath) || WildCardRuleMatching.findMatchingRuleWithWildCards(accumulatedJsonPath)(r._1)
      }.map(kvp => MatchingRuleContext(kvp._1.replace("$.body", ""), kvp._2)).toList
      //)
    } else Nil

  def matchNodeWithRules(matchingRules: Map[String, MatchingRule], accumulatedXmlPath: String, ex: IrNode, re: IrNode): ArrayMatchingStatus = {
    val rules =
      matchingRules.filter { mr =>
        val mrPath = mr._1.replace("$.body.", "").replace("$.body", "")

        // To account for paths that start at the root.
        mrPath.startsWith(accumulatedXmlPath) || mrPath.isEmpty || mrPath.startsWith("[")
      }

    val results =
      rules
        .map(rule => (rule._1.replace("$.body.", "").replace("$.body", "").replace(accumulatedXmlPath, ""), rule._2))
        .map(kvp => traverseAndMatch(kvp._1, kvp._2, ex, re))
        .toList

    ArrayMatchingStatus.listArrayMatchStatusToSingle(results)
  }

  // This is best effort type checking, not ideal.
  // Eventually we just have to say that it passes on the assumption, having ruled out
  // other options, that we have two arbitrary strings.
  def typeCheck(ex: IrNode, re: IrNode): ArrayMatchingStatus = {

    // Received node contains same attributes as expected
    val attributesOk = if(mapContainsMapIgnoreValues(ex.attributes.asAttrMap, re.attributes.asAttrMap)) RuleMatchSuccess else RuleMatchFailure

    // Children are all the same type
    val childrenLabel = ex.child.headOption.map(_.label)

    // No label means no children, so that's a success
    val nodeChildrenCheck = childrenLabel.map { lbl =>
      if(re.child.forall(c => c.label == lbl)) RuleMatchSuccess else RuleMatchFailure
    }.getOrElse(RuleMatchSuccess)

    // If this is a leaf node, check the types align
    // This always works (strangely it may seem..) because it fails
    // successfully assuming an arbitrary string.
    val leafCheck =
    ex.text match {
      case x if x.isEmpty => // Empty
        if(re.text.isEmpty) RuleMatchSuccess else RuleMatchFailure
      case x if x.matches(isNumericValueRegex) => // Any numeric, can be negative, can have decimal places
        if(re.text.matches(isNumericValueRegex)) RuleMatchSuccess else RuleMatchFailure
      case x if x.matches(isBooleanValueRegex) => // Any Boolean
        if(re.text.matches(isBooleanValueRegex)) RuleMatchSuccess else RuleMatchFailure
      case _ => // Finally, any arbitrary string, but we can attempt to rule out numbers and booleans
        if(re.text.matches(isNumericValueRegex) || re.text.matches(isBooleanValueRegex)) RuleMatchFailure
        else RuleMatchSuccess
    }

    ArrayMatchingStatus.listArrayMatchStatusToSingle(List(attributesOk, nodeChildrenCheck, leafCheck))
  }

  def regexCheck(regex: String, re: IrNode): ArrayMatchingStatus =
    if(re.text.matches(regex)) RuleMatchSuccess else RuleMatchFailure

  def traverseAndMatch(remainingRulePath: String, rule: MatchingRule, ex: IrNode, re: IrNode): ArrayMatchingStatus = {
    remainingRulePath match {
      case rp: String if rp.startsWith(".@") =>
        //        println(s"Found attribute path: '$rp'".yellow)
        val attributeKey = rp.replace(".@", "")

        ex.attributes.asAttrMap.get(attributeKey).map { attributeValue =>
          if(attributeRuleTest(re.attributes.asAttrMap, attributeKey -> attributeValue)(rule)) RuleMatchSuccess
          else RuleMatchFailure
        }.getOrElse {
          //          println(s"Could not extract attribute from expected IrNode.".yellow)
          RuleMatchFailure
        }

      case rp: String if rp.isEmpty =>
        //Seems odd, but this basically means the rule must be applied to the current node.
        rule match {
          case MatchingRule(Some(ruleType), _, _) if ruleType == "type" =>
            //            println(s"Type rule for empty rule path found.".yellow)
            typeCheck(ex, re)

          case MatchingRule(Some(ruleType), _, Some(min)) if ruleType == "type" =>
            //            println(s"Type rule with min found.".yellow)

            val res = List(
              typeCheck(ex, re),
              if(re.child.length >= min) RuleMatchSuccess else RuleMatchFailure
            )

            ArrayMatchingStatus.listArrayMatchStatusToSingle(res)

          case MatchingRule(Some(ruleType), Some(regex), _) if ruleType == "regex" =>
            //            println(s"Regex rule found.".yellow)
            regexCheck(regex, re)

          case MatchingRule(Some(_), None, _) =>
            //            println(s"Regex rule found but no pattern supplied.".yellow)
            RuleMatchFailure

          case MatchingRule(_, _, Some(min)) =>
            //            println(s"Array length min check".yellow)
            if(re.child.length >= min) RuleMatchSuccess else RuleMatchFailure

          case unexpectedRule =>
            //            println(s"Unexpected leaf rule: $unexpectedRule".yellow)
            RuleMatchFailure
        }

      case rp: String if rp.matches("^\\.[a-zA-Z].*") =>
        //        println(s"Found field rule path: '$rp'".yellow)

        val maybeFieldName = """\w+""".r.findFirstIn(rp)
        val leftOverPath = """\.\w+""".r.replaceFirstIn(rp, "").replace(".#text", "")

        if(leftOverPath.startsWith("[")) {
          traverseAndMatch(leftOverPath, rule, ex, re)
        } else {
          maybeFieldName.flatMap { fieldName =>
            for {
              e <- ex.child.find(n => n.label == fieldName)
              r <- re.child.find(n => n.label == fieldName)
            } yield traverseAndMatch(leftOverPath, rule, e, r)
          }.getOrElse(RuleMatchFailure)
        }

      case rp: String if rp.matches("""^\[\d+\].*""") || rp.matches("""\.\d+""") =>
        //        println(s"Found array rule path: '$rp'".yellow)

        val index: Int = """\d+""".r.findFirstIn(rp).flatMap(Helpers.safeStringToInt).getOrElse(-1)
        val leftOverPath = """(\.?)(\[?)\d+(\]?)""".r.replaceFirstIn(remainingRulePath, "")
        val cleanLeftOverPath = """\.\w+""".r.replaceFirstIn(leftOverPath, "").replace(".#text", "")

        ex.child.headOption.flatMap { e =>
          re.child.drop(index).headOption.flatMap { r =>
            Option(traverseAndMatch(cleanLeftOverPath, rule, e, r))
          }
        }.getOrElse {
          //           println(s"Received XMl was missing a child array node at position: $index".yellow)
          RuleMatchFailure
        }

      case rp: String if rp.matches("""^\[\*\].*""") =>
        //        println(s"Found array wildcard rule path: '$rp'".yellow)

        val leftOverPath = """^\[\*\]""".r.replaceFirstIn(remainingRulePath, "")
        val cleanLeftOverPath = """\.\w+""".r.replaceFirstIn(leftOverPath, "").replace(".#text", "")

        ex.child.headOption.map { en =>
          re.child.map { rn =>
            traverseAndMatch(cleanLeftOverPath, rule, en, rn)
          }
        }.map { l =>
          ArrayMatchingStatus.listArrayMatchStatusToSingle(l.toList)
        }.getOrElse {
          //           println(s"Expected was missing a child array node at position 0".yellow)
          RuleMatchFailure
        }

      case rp: String if rp.matches("""^\.\*.*""") =>
        //        println(s"Found array wildcard rule path: '$rp'".yellow)

        val leftOverPath = """^\.\*""".r.replaceFirstIn(remainingRulePath, "")

        traverseAndMatch(leftOverPath, rule, ex, re)

      case rp: String =>
        //        println(s"Unexpected branch rule path: '$rp'".yellow)
        RuleMatchFailure
    }
  }

}
*/