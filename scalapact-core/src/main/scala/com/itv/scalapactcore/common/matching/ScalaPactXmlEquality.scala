package com.itv.scalapactcore.common.matching

import com.itv.scalapactcore.MatchingRule
import com.itv.scalapactcore.common.matching.BodyMatching.BodyMatchingRules
import com.itv.scalapactcore.common.Helpers

import scala.language.implicitConversions
import scala.xml.{Elem, Node}
import scalaz.Scalaz._

import com.itv.scalapactcore.common.ColourOuput._

object ScalaPactXmlEquality {

  implicit def toXmlEqualityWrapper(xmlElem: Elem): XmlEqualityWrapper = XmlEqualityWrapper(xmlElem)

  case class XmlEqualityWrapper(xml: Elem) {
    def =~(to: Elem): BodyMatchingRules => Boolean = matchingRules =>
      PermissiveXmlEqualityHelper.areEqual(xmlPathToJsonPath(matchingRules), xml, to, xml.label)
    def =<>=(to: Elem): Boolean => BodyMatchingRules => Boolean = beSelectivelyPermissive => matchingRules =>
      StrictXmlEqualityHelper.areEqual(beSelectivelyPermissive, xmlPathToJsonPath(matchingRules), xml, to, xml.label)
  }

  private val xmlPathToJsonPath: BodyMatchingRules => BodyMatchingRules = matchingRules =>
    matchingRules.map { mrs =>
      mrs.map { mr =>
        (mr._1.replaceAll("""\[\'""", ".").replaceAll("""\'\]""", ""), mr._2)
      }
    }

}

object StrictXmlEqualityHelper {

  def areEqual(beSelectivelyPermissive: Boolean, matchingRules: BodyMatchingRules, expected: Elem, received: Elem, accumulatedXmlPath: String): Boolean = {
    (expected.headOption |@| received.headOption) { (e, r) => compareNodes(beSelectivelyPermissive)(matchingRules)(e)(r)(accumulatedXmlPath) } match {
      case Some(bool) => bool
      case None => false
    }
  }

  lazy val compareNodes: Boolean => BodyMatchingRules => Node => Node => String => Boolean = beSelectivelyPermissive => matchingRules => expected => received => accumulatedXmlPath => {

    SharedXmlEqualityHelpers.matchNodeWithRules(matchingRules)(accumulatedXmlPath)(expected)(received) match {
      case RuleMatchSuccess => true
      case RuleMatchFailure => false
      case NoRuleMatchRequired =>
        lazy val prefixEqual = expected.prefix == received.prefix
        lazy val labelEqual = expected.label == received.label
        lazy val attributesLengthOk = expected.attributes.length == received.attributes.length
        lazy val attributesEqual = SharedXmlEqualityHelpers.checkAttributeEquality(matchingRules)(accumulatedXmlPath)(expected.attributes.asAttrMap)(received.attributes.asAttrMap)
        lazy val childLengthOk = expected.child.length == received.child.length

        lazy val childrenEqual =
          if(expected.child.isEmpty) expected.text == received.text
          else {
            expected.child.zip(received.child).forall(p => compareNodes(beSelectivelyPermissive)(matchingRules)(p._1)(p._2)(accumulatedXmlPath + "." + expected.label))
          }

        prefixEqual && labelEqual && attributesLengthOk && attributesEqual && childLengthOk && childrenEqual
    }
  }

}

object PermissiveXmlEqualityHelper {

  /***
    * Permissive equality means that the elements and fields defined in the 'expected'
    * are required to be present in the 'received', however, extra elements on the right
    * are allowed and ignored. Additionally elements are still considered equal if their
    * fields or array elements are out of order, as long as they are present since json
    * doesn't not guarantee element order.
    */
  def areEqual(matchingRules: BodyMatchingRules, expected: Elem, received: Elem, accumulatedXmlPath: String): Boolean = {
    (expected.headOption |@| received.headOption) { (e, r) => compareNodes(matchingRules)(e)(r)(accumulatedXmlPath) } match {
      case Some(bool) => bool
      case None => false
    }
  }

  lazy val compareNodes: BodyMatchingRules => Node => Node => String => Boolean = matchingRules => expected => received => accumulatedXmlPath => {

    SharedXmlEqualityHelpers.matchNodeWithRules(matchingRules)(accumulatedXmlPath)(expected)(received) match {
      case RuleMatchSuccess => true
      case RuleMatchFailure => false
      case NoRuleMatchRequired =>
        lazy val prefixEqual = expected.prefix == received.prefix
        lazy val labelEqual = expected.label == received.label
        lazy val attributesEqual = SharedXmlEqualityHelpers.checkAttributeEquality(matchingRules)(accumulatedXmlPath)(expected.attributes.asAttrMap)(received.attributes.asAttrMap)
        lazy val childLengthOk = expected.child.length <= received.child.length

        lazy val childrenEqual =
          if(expected.child.isEmpty) expected.text == received.text
          else expected.child.forall { eN => received.child.exists(rN => compareNodes(matchingRules)(eN)(rN)(accumulatedXmlPath + "." + eN.label)) }

        prefixEqual && labelEqual && attributesEqual && childLengthOk && childrenEqual
    }
  }

}

object SharedXmlEqualityHelpers {

  lazy val checkAttributeEquality: BodyMatchingRules => String => Map[String, String] => Map[String, String] => Boolean = matchingRules => accumulatedXmlPath => e => r => {

    val rulesMap = e.flatMap { ex =>
      Map(ex._1 -> findMatchingRules(accumulatedXmlPath + ".@" + ex._1)(matchingRules).getOrElse(Nil))
    }.filter(_._2.nonEmpty)

    val (attributesWithRules, attributesWithoutRules) = e.partition(p => rulesMap.contains(p._1))

    if(attributesWithRules.isEmpty){
      SharedXmlEqualityHelpers.mapContainsMap(e)(r)
    } else {
      attributesWithRules.forall(attributeRulesCheck(rulesMap)(r)) && mapContainsMap(attributesWithoutRules)(r)
    }
  }

  lazy val mapContainsMap: Map[String, String] => Map[String, String] => Boolean = e => r =>
    e.forall { ee =>
      r.exists(rr => rr._1 == ee._1 && rr._2 == ee._2)
    }

  private lazy val attributeRulesCheck: Map[String, List[MatchingRuleContext]] => Map[String, String] => ((String, String)) => Boolean = rulesMap => receivedAttributes => attribute =>
    rulesMap(attribute._1).map(_.rule).forall(attributeRuleTest(receivedAttributes)(attribute))

  private lazy val attributeRuleTest: Map[String, String] => ((String, String)) => MatchingRule => Boolean = receivedAttributes => attribute => rule => {
    val a = MatchingRule.unapply(rule)

    MatchingRule.unapply(rule) match {
      case Some((Some(matchType), None, None)) if matchType == "type" =>
        true //Always a string? Hmmmmm... Attributes are strings but they contain other types.

      case Some((Some(matchType), Some(regex), _)) if matchType == "regex" =>
        receivedAttributes
          .get(attribute._1)
          .map(_.matches(regex))
          .getOrElse(false)

      case _ =>
        println("Unexpected match type during attribute matching with rule".red)
        false

    }
  }

  private val findMatchingRules: String => BodyMatchingRules => Option[List[MatchingRuleContext]] = accumulatedJsonPath => m =>
    if (accumulatedJsonPath.length > 0) {
      m.flatMap(
        _.map(r => (r._1.replace("['", ".").replace("']", ""), r._2)).filter { r =>
          r._1.endsWith(accumulatedJsonPath) || WildCardRuleMatching.findMatchingRuleWithWildCards(accumulatedJsonPath)(r._1)
        }.map(kvp => MatchingRuleContext(kvp._1.replace("$.body", ""), kvp._2)).toList.some
      )
    } else None

  val matchNodeWithRules: BodyMatchingRules => String => Node => Node => ArrayMatchingStatus = matchingRules => accumulatedXmlPath => ex => re => {

    val rules = matchingRules.map { mrs =>
      mrs.filter(mr => mr._1.replace("$.body.", "").startsWith(accumulatedXmlPath))
    }.getOrElse(Map.empty[String, MatchingRule])

    val results =
      rules
        .map(rule => (rule._1.replace("$.body.", "").replace(accumulatedXmlPath, ""), rule._2))
        .map(kvp => traverseAndMatch(kvp._1)(kvp._2)(ex)(re))
        .toList

    ArrayMatchingStatus.listArrayMatchStatusToSingle(results)
  }
  // Maybe negative, must have digits, may have decimal and if so must have a
  // digit after it, can have more trailing digits.
  val isNumericValueRegex = """(^-?)(\d+)(\.?\d)(\d*)"""
  val isBooleanValueRegex = """true|false"""

  // This is best effort type checking, not ideal.
  // Eventually we just have to say that it passes on the assumption, having ruled out
  // other options, that we have two arbitrary strings.
  val typeCheck: Node => Node => ArrayMatchingStatus = ex => re =>
    ex.text match {
      case x if x.isEmpty => // Any numeric
        if(re.text.isEmpty) RuleMatchSuccess else RuleMatchFailure
      case x if x.matches(isNumericValueRegex) => // Any numeric, can be negative, can have decimal places
        if(re.text.matches(isNumericValueRegex)) RuleMatchSuccess else RuleMatchFailure
      case x if x.matches(isBooleanValueRegex) => // Any Boolean
        if(re.text.matches(isBooleanValueRegex)) RuleMatchSuccess else RuleMatchFailure
      case x => // Finally, any arbitrary string
        RuleMatchSuccess
    }

  val regexCheck: String => Node => ArrayMatchingStatus = regex => re =>
    if(re.text.matches(regex)) RuleMatchSuccess else RuleMatchFailure

  val traverseAndMatch: String => MatchingRule => Node => Node => ArrayMatchingStatus = remainingRulePath => rule => ex => re => {

    remainingRulePath match {
      case rp: String if rp.startsWith(".@") =>
        // println(s"Found attribute path: '$rp'".yellow)
        val attribute = (rp.replace(".@", ""), "")

        if(attributeRuleTest(re.attributes.asAttrMap)(attribute)(rule)) RuleMatchSuccess
        else RuleMatchFailure

      case rp: String if rp.isEmpty =>
        //Seems odd, but this basically means the rule must be applied to the current node.
        //TODO: Very similar to below... refactor?
        rule match {
          case MatchingRule(Some(ruleType), _, _) if ruleType == "type" =>
            // println(s"Type rule found.".yellow)
            typeCheck(ex)(re)

          case MatchingRule(Some(ruleType), _, Some(min)) if ruleType == "type" =>
            // println(s"Type rule found.".yellow)

            val res = List(
             typeCheck(ex)(re),
             if(re.child.length >= min) RuleMatchSuccess else RuleMatchFailure
            )

            ArrayMatchingStatus.listArrayMatchStatusToSingle(res)

          case MatchingRule(Some(ruleType), Some(regex), _) if ruleType == "regex" =>
            // println(s"Regex rule found.".yellow)
            regexCheck(regex)(re)

          case MatchingRule(Some(ruleType), None, _) =>
            // println(s"Regex rule found but no pattern supplied.".yellow)
            RuleMatchFailure

          case MatchingRule(_, _, Some(min)) =>
            // println(s"Array length min check".yellow)
            if(re.child.length >= min) RuleMatchSuccess else RuleMatchFailure

          case unexpectedRule =>
            // println(s"Unexpected leaf rule: $unexpectedRule".yellow)
            RuleMatchFailure
        }

      case rp: String if rp.matches("^.[a-zA-Z].*") =>
        // println(s"Found field rule path: '$rp'".yellow)

        val maybeFieldName = """\w+""".r.findFirstIn(rp)
        val leftOverPath = """\.\w+""".r.replaceFirstIn(rp, "")
        maybeFieldName.map { fieldName =>

          if(fieldName == ex.label && fieldName == re.label) {
            if(leftOverPath.isEmpty) {
              rule match {
                case MatchingRule(Some(ruleType), _, _) if ruleType == "type" =>
                  // println(s"Type rule found.".yellow)
                  typeCheck(ex)(re)

                case MatchingRule(Some(ruleType), Some(regex), _) if ruleType == "regex" =>
                  // println(s"Regex rule found.".yellow)
                  regexCheck(regex)(re)

                case MatchingRule(Some(ruleType), None, _) =>
                  // println(s"Regex rule found but no pattern supplied.".yellow)
                  RuleMatchFailure

                case MatchingRule(_, _, Some(min)) =>
                  // println(s"Invalid rule, tried to test array min of $min on a leaf node".yellow)
                  RuleMatchFailure

                case unexpectedRule =>
                  // println(s"Unexpected leaf rule: $unexpectedRule".yellow)
                  RuleMatchFailure

              }
            }
            else traverseAndMatch(leftOverPath)(rule)(ex)(re)
          }
          else RuleMatchFailure

        }.getOrElse {
          println(s"Expected or Received XMl was missing a field node: $maybeFieldName".yellow)
          RuleMatchFailure
        }

      case rp: String if rp.matches("""^\[\d+\].*""") || rp.matches("""\.\d+""") =>
        //println(s"Found array rule path: '$rp'".yellow)

        val index: Int = """\d+""".r.findFirstIn(rp).flatMap(Helpers.safeStringToInt).getOrElse(-1)
        val leftOverPath = """(\.?)(\[?)\d+(\]?)""".r.replaceFirstIn(remainingRulePath, "")

        (ex.child.headOption |@| re.child.drop(index).headOption) { (e, r) =>
          traverseAndMatch(leftOverPath)(rule)(e)(r)
        }.getOrElse {
          println(s"Received XMl was missing a child array node at position: $index".yellow)
          RuleMatchFailure
        }

      case rp: String if rp.matches("""^\[\*\].+""") =>
        // println(s"Found array wildcard rule path: '$rp'".yellow)

        val leftOverPath = """^\[\*\]""".r.replaceFirstIn(remainingRulePath, "")

        ex.child.headOption.map { en =>
          re.child.map { rn =>
            traverseAndMatch(leftOverPath)(rule)(en)(rn)
          }
        }.map { l =>
          ArrayMatchingStatus.listArrayMatchStatusToSingle(l.toList)
        }.getOrElse {
          println(s"Expected was missing a child array node at position 0".yellow)
          RuleMatchFailure
        }

      case rp: String =>
        // println(s"Unexpected branch rule path: '$rp'".yellow)
        RuleMatchFailure
    }
  }

}
