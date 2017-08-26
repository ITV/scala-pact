package com.itv.scalapactcore.common.matchir

import scala.annotation.tailrec
import scala.language.postfixOps
import scala.util.matching.Regex

/**
  * PactPath (defined in the pact standard) is JsonPath with a few tweaks to support
  * querying XML with a nearly JsonPath-like syntax. Specific modifications to JsonPath are:
  *
  * - names match to element names ($.body.animals maps to <animals>)
  * - @names match to attribute names
  * - #text match to the text elements
  *
  * JsonPath support a ["xxx"] form which we use for to escape the @ and #. e.g.
  * foo.bar["#text"]
  * foo.bar['@id']
  */
object PactPath {

  import PactPathPatterns._
  import PactPathParseResult._

  val toPactPath: IrNodePath => String = _.renderAsString

  val fromPactPath: String => PactPathParseResult = pactPath => {
    def rec(remaining: String, acc: IrNodePath): PactPathParseResult =
      remaining match {
        // Complete, success
        case "" =>
          PactPathParseSuccess(acc)

        // Fields
        case fieldNamePrefix(_, r) =>
          rec(r, acc)

        case fieldName(name, r) =>
          rec(r, acc <~ name)

        case fieldNameSuffix(_, r) =>
          rec(r, acc)

        // Arrays
        case arrayAnyElement(_, r) =>
          rec(r, acc <~ "*")

        case arrayElementAtIndex(i, r) =>
          safeStringToInt(i) match {
            case Some(index) =>
              rec(r, acc <~ index)

            case None =>
              PactPathParseFailure(pactPath, remaining, Some(s"Could not convert '$i' to array index."))
          }

        // XML Addons
        case xmlAttributeName(attributeName, r) =>
          rec(r, acc <@ attributeName)

        case xmlTextElement(_, r) =>
          rec(r, acc text)

        // Unmatched, failure
        case _ =>
          println("b")
          PactPathParseFailure(pactPath, remaining, None)

      }

    rec(pactPath, IrNodePathEmpty)
  }

  private lazy val safeStringToInt: String => Option[Int] = str => try { Option(str.toInt) } catch { case _: Throwable => None }

}

sealed trait PactPathParseResult

object PactPathParseResult {
  case class PactPathParseSuccess(irNodePath: IrNodePath) extends PactPathParseResult
  case class PactPathParseFailure(original: String, lookingAt: String, specificError: Option[String]) extends PactPathParseResult {
    def errorString: String =
      s"${specificError.getOrElse("Failed to parse PactPath")}. Was looking at '$lookingAt' in '$original'"
  }
}

object PactPathPatterns {
  // Fields
  val fieldNamePrefix: Regex ="""^(\.|\[[\'|\"])(.*)$""".r
  val fieldName: Regex = """^([a-zA-Z0-9:\-_]+)(.*)$""".r // TODO: This is very limited, technically any escaped string is valid
  val fieldNameSuffix: Regex = """^([\'|\"]\])(.*)$""".r

  // Arrays
  val arrayAnyElement: Regex = """^(\[\*\])(.*)$""".r
  val arrayElementAtIndex: Regex = """^\[(\d+)\](.*)$""".r

  // Xml addons
  val xmlAttributeName: Regex = """^@([a-zA-Z0-9:\-_]+)(.*)$""".r
  val xmlTextElement: Regex = """^#([a-zA-Z0-9:\-_]+)(.*)$""".r
}

object IrNodePath {
  val fromPactPath: String => PactPathParseResult = PactPath.fromPactPath
}

sealed trait IrNodePath {
  def <~(fieldName: String): IrNodePath =
    if(fieldName == "*") IrNodePathArrayAnyElement(this) else IrNodePathField(fieldName, this)

  def <~(arrayIndex: Int): IrNodePath = IrNodePathArrayElement(arrayIndex, this)

  def <@(attributeName: String): IrNodePath = IrNodePathFieldAttribute(attributeName, this)

  def text: IrNodePath = IrNodePathTextElement(this)

  def =~=(other: IrNodePath): Boolean = isEqualTo(other, strict = false)
  def ===(other: IrNodePath): Boolean = isEqualTo(other, strict = true)

  def isEqualTo(other: IrNodePath, strict: Boolean): Boolean = {
    @tailrec
    def rec(a: IrNodePath, b: IrNodePath): Boolean =
      (a, b) match {
        case (IrNodePathEmpty, IrNodePathEmpty) =>
          true

        case (IrNodePathField(fieldNameA, parentA), IrNodePathField(fieldNameB, parentB)) if fieldNameA == fieldNameB =>
          rec(parentA, parentB)

        case (IrNodePathArrayElement(indexA, parentA), IrNodePathArrayElement(indexB, parentB)) if strict && indexA == indexB =>
          rec(parentA, parentB)

        case (IrNodePathArrayElement(_, parentA), IrNodePathArrayElement(_, parentB)) if !strict =>
          rec(parentA, parentB)

        case (IrNodePathArrayAnyElement(parentA), IrNodePathArrayAnyElement(parentB)) =>
          rec(parentA, parentB)

        case (IrNodePathArrayAnyElement(parentA), IrNodePathArrayElement(_, parentB)) =>
          rec(parentA, parentB)

        case (IrNodePathFieldAttribute(attributeNameA, parentA), IrNodePathFieldAttribute(attributeNameB, parentB)) if attributeNameA == attributeNameB =>
          rec(parentA, parentB)

        case (IrNodePathTextElement(parentA), IrNodePathTextElement(parentB)) =>
          rec(parentA, parentB)

        case (IrNodePathArrayElement(_, parentA), IrNodePathArrayAnyElement(parentB)) =>
          rec(parentA, parentB)

        case _ =>
          false
      }

    rec(this, other)
  }

  val toPactPath: String = renderAsString

  def renderAsString: String = {
    def rec(irNodePath: IrNodePath, acc: String): String =
      irNodePath match {
        case p @ IrNodePathEmpty if acc.isEmpty =>
          p.name

        case IrNodePathEmpty =>
          if(acc.startsWith(".[")) acc.drop(1) else acc

        case IrNodePathField(fieldName, parent) =>
          if(fieldName != MatchIr.unnamedNodeLabel && fieldName != MatchIr.rootNodeLabel)
            rec(parent, s".$fieldName$acc")
          else
            rec(parent, s".$acc")

        case IrNodePathArrayElement(arrayIndex, parent) =>
          rec(parent, s"[$arrayIndex]$acc")

        case IrNodePathArrayAnyElement(parent) =>
          rec(parent, s"[*]$acc")

        case IrNodePathFieldAttribute(attributeName, parent) =>
          rec(parent, s"['@$attributeName']$acc")

        case IrNodePathTextElement(parent) =>
          rec(parent, s"['#text']$acc")
      }

    rec(this, "")
  }
}

case object IrNodePathEmpty extends IrNodePath {
  val name: String = "."
}
case class IrNodePathField(fieldName: String, parent: IrNodePath) extends IrNodePath
case class IrNodePathArrayElement(index: Int, parent: IrNodePath) extends IrNodePath
case class IrNodePathArrayAnyElement(parent: IrNodePath) extends IrNodePath
case class IrNodePathFieldAttribute(attributeName: String, parent: IrNodePath) extends IrNodePath
case class IrNodePathTextElement(parent: IrNodePath) extends IrNodePath
