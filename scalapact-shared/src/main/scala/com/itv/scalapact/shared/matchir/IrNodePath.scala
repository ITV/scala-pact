package com.itv.scalapact.shared.matchir

import com.itv.scalapact.shared.matchir.PactPathParseResult.{PactPathParseFailure, PactPathParseSuccess}

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

        // Prefixes
        case dollarPrefix(_, r) =>
          rec(r, acc)

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

        // Suffixes
        case anyField(_, r) =>
          rec(r, acc <*)

        // Unmatched, failure
        case _ =>
          PactPathParseFailure(pactPath, remaining, None)

      }

    rec(pactPath, IrNodePathEmpty)
  }

  private lazy val safeStringToInt: String => Option[Int] = str => try { Option(str.toInt) } catch { case _: Throwable => None }

}

sealed trait PactPathParseResult {

  def toOption: Option[IrNodePath] =
    this match {
      case PactPathParseSuccess(p) =>
        Option(p)

      case _ =>
        None
    }

  def toEither: Either[String, IrNodePath] =
    this match {
      case PactPathParseSuccess(p) =>
        Right(p)

      case e: PactPathParseFailure =>
        Left(e.errorString)
    }

}

object PactPathParseResult {
  case class PactPathParseSuccess(irNodePath: IrNodePath) extends PactPathParseResult
  case class PactPathParseFailure(original: String, lookingAt: String, specificError: Option[String]) extends PactPathParseResult {
    def errorString: String =
      s"${specificError.getOrElse("Failed to parse PactPath")}. Was looking at '$lookingAt' in '$original'"
  }
}

object PactPathPatterns {
  // Prefixes
  val dollarPrefix: Regex ="""^(\$.body|\$.headers)(.*)$""".r

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

  // Suffixes
  val anyField: Regex ="""^(\*)(.*)$""".r
}

sealed trait IrNodePath {

  def isEmpty: Boolean
  def isField: Boolean
  def isArrayIndex: Boolean
  def isArrayWildcard: Boolean
  def isAttribute: Boolean
  def isTextElement: Boolean
  def isAnyField: Boolean

  def giveArrayIndex: Int

  def <~(fieldName: String): IrNodePath =
    if(fieldName == "*") IrNodePathArrayAnyElement(this) else IrNodePathField(fieldName, this)

  def <~(arrayIndex: Int): IrNodePath = IrNodePathArrayElement(arrayIndex, this)

  def <@(attributeName: String): IrNodePath = IrNodePathFieldAttribute(attributeName, this)

  def text: IrNodePath = IrNodePathTextElement(this)

  def <* : IrNodePath = IrNodePathAnyField(this)

  def =~=(other: IrNodePath): Boolean = isEqualTo(other, strict = false)
  def ===(other: IrNodePath): Boolean = isEqualTo(other, strict = true)

  def noText: IrNodePath = if(this.isTextElement) this.parent else this

  def ++(other: IrNodePath): IrNodePath = append(other)

  def append(other: IrNodePath): IrNodePath = IrNodePath.append(this, other)

  def invert: IrNodePath = IrNodePath.invert(this)

  val parent: IrNodePath

  def withParent(path: IrNodePath): IrNodePath = IrNodePath.withParent(this, path)

  def withParentAtRoot(newRootParent: IrNodePath): IrNodePath =
    this match {
      case IrNodePathEmpty =>
        newRootParent

      case seg @ IrNodePathField(_, _) =>
        seg.withParent(seg.parent.withParentAtRoot(newRootParent))

      case seg @ IrNodePathArrayElement(_, _) =>
        seg.withParent(seg.parent.withParent(seg.parent.parent.withParentAtRoot(newRootParent)))

      case seg @ IrNodePathArrayAnyElement(_) =>
        seg.withParent(seg.parent.withParent(seg.parent.parent.withParentAtRoot(newRootParent)))

      case seg @ IrNodePathFieldAttribute(_, _) =>
        seg.withParent(seg.parent.withParent(seg.parent.parent.withParentAtRoot(newRootParent)))

      case seg @ IrNodePathTextElement(_) =>
        seg.withParent(seg.parent.withParentAtRoot(newRootParent))

      case seg @ IrNodePathAnyField(_) =>
        seg.withParent(seg.parent.withParent(seg.parent.parent.withParentAtRoot(newRootParent)))
    }

  def isEqualTo(other: IrNodePath, strict: Boolean): Boolean = IrNodePath.areEqual(this, other, strict)

  val toPactPath: String = renderAsString

  def withIndexes: List[IrNodePath] = IrNodePath.withIndexes(this)

  def split: List[IrNodePath] = IrNodePath.split(this)

  def renderAsString: String = IrNodePath.renderAsString(this)

  def lastSegmentLabel: String =
    split
      .reverse
      .headOption
      .map { s =>
        if(s.isField || s.isAttribute) s
        else s.parent
      }
      .map(_.renderAsString)
      .getOrElse(IrNodePathEmpty.name)
}

case object IrNodePathEmpty extends IrNodePath {
  val name: String = "."
  val parent: IrNodePath = IrNodePathEmpty
  def isEmpty: Boolean = true
  def isField: Boolean = false
  def isArrayIndex: Boolean = false
  def isArrayWildcard: Boolean = false
  def isAttribute: Boolean = false
  def isTextElement: Boolean = false
  def isAnyField: Boolean = false
  def giveArrayIndex: Int = 0
}
case class IrNodePathField(fieldName: String, parent: IrNodePath) extends IrNodePath {
  def isEmpty: Boolean = false
  def isField: Boolean = true
  def isArrayIndex: Boolean = false
  def isArrayWildcard: Boolean = false
  def isAttribute: Boolean = false
  def isTextElement: Boolean = false
  def isAnyField: Boolean = false
  def giveArrayIndex: Int = 0
}
case class IrNodePathArrayElement(index: Int, parent: IrNodePath) extends IrNodePath {
  def isEmpty: Boolean = false
  def isField: Boolean = false
  def isArrayIndex: Boolean = true
  def isArrayWildcard: Boolean = false
  def isAttribute: Boolean = false
  def isTextElement: Boolean = false
  def isAnyField: Boolean = false
  def giveArrayIndex: Int = index
}
case class IrNodePathArrayAnyElement(parent: IrNodePath) extends IrNodePath {
  def isEmpty: Boolean = false
  def isField: Boolean = false
  def isArrayIndex: Boolean = false
  def isArrayWildcard: Boolean = true
  def isAttribute: Boolean = false
  def isTextElement: Boolean = false
  def isAnyField: Boolean = false
  def giveArrayIndex: Int = 0
}
case class IrNodePathFieldAttribute(attributeName: String, parent: IrNodePath) extends IrNodePath {
  def isEmpty: Boolean = false
  def isField: Boolean = false
  def isArrayIndex: Boolean = false
  def isArrayWildcard: Boolean = false
  def isAttribute: Boolean = true
  def isTextElement: Boolean = false
  def isAnyField: Boolean = false
  def giveArrayIndex: Int = 0
}
case class IrNodePathTextElement(parent: IrNodePath) extends IrNodePath {
  def isEmpty: Boolean = false
  def isField: Boolean = false
  def isArrayIndex: Boolean = false
  def isArrayWildcard: Boolean = false
  def isAttribute: Boolean = false
  def isTextElement: Boolean = true
  def isAnyField: Boolean = false
  def giveArrayIndex: Int = 0
}

case class IrNodePathAnyField(parent: IrNodePath) extends IrNodePath {
  def isEmpty: Boolean = false
  def isField: Boolean = false
  def isArrayIndex: Boolean = false
  def isArrayWildcard: Boolean = false
  def isAttribute: Boolean = false
  def isTextElement: Boolean = false
  def isAnyField: Boolean = true
  def giveArrayIndex: Int = 0
}

object IrNodePath {

  val empty: IrNodePath = IrNodePathEmpty

  val fromPactPath: String => PactPathParseResult = PactPath.fromPactPath

  def invert(path: IrNodePath): IrNodePath =
    IrNodePath.combine(path.split.reverse)

  def withParent(path: IrNodePath, newParent: IrNodePath): IrNodePath = {
    path match {
      case IrNodePathEmpty =>
        path

      case seg @ IrNodePathField(_, _) =>
        seg.copy(parent = newParent)

      case seg @ IrNodePathArrayElement(_, _) =>
        seg.copy(parent = newParent)

      case seg @ IrNodePathArrayAnyElement(_) =>
        seg.copy(parent = newParent)

      case seg @ IrNodePathFieldAttribute(_, _) =>
        seg.copy(parent = newParent)

      case seg @ IrNodePathTextElement(_) =>
        seg.copy(parent = newParent)

      case seg @ IrNodePathAnyField(_) =>
        seg.copy(parent = newParent)
    }
  }

  def append(pathA: IrNodePath, pathB: IrNodePath): IrNodePath =
    pathB.withParentAtRoot(pathA)

  def areEqual(pathA: IrNodePath, pathB: IrNodePath, strict: Boolean): Boolean = {
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

        case (IrNodePathAnyField(parentA), IrNodePathAnyField(parentB)) =>
          rec(parentA, parentB)

        case (IrNodePathAnyField(parentA), IrNodePathField(_, parentB)) =>
          rec(parentA, parentB)

        case _ =>
          false
      }

    rec(pathA, pathB)
  }

  def withIndexes(path: IrNodePath): List[IrNodePath] = {
    def rec(segments: List[IrNodePath], indexPathSoFar: IrNodePath, acc: List[IrNodePath]): List[IrNodePath] =
      segments match {
        case Nil =>
          acc

        case x :: xs if x.isField =>
          val p = indexPathSoFar ++ (x <~ 0)
          val next = p ++ IrNodePath.combine(xs)
          rec(xs, p, next :: acc)

        case x :: xs =>
          val p = indexPathSoFar ++ x
          val next = p ++ IrNodePath.combine(xs)
          rec(xs, p, if (acc.exists(_ === next)) acc else next :: acc)
      }

    def deduplicate(remaining: List[IrNodePath], acc: List[IrNodePath]): List[IrNodePath] =
      remaining match {
        case Nil =>
          acc

        case x :: xs if acc.exists(_ === x) =>
          deduplicate(xs, acc)

        case x :: xs =>
          deduplicate(xs, x :: acc)

      }

    deduplicate(rec(path.invert.split, IrNodePath.empty, Nil).map(_.invert) ++ rec(path.split, IrNodePath.empty, Nil), Nil)
  }

  def split(path: IrNodePath): List[IrNodePath] = {
    def rec(p: IrNodePath, acc: List[IrNodePath]): List[IrNodePath] = {
      p match {
        case IrNodePathEmpty =>
          acc

        case seg @ IrNodePathField(_, _) =>
          rec(p.parent, seg.withParent(IrNodePath.empty) :: acc)

        case seg @ IrNodePathArrayElement(_, _) =>
          rec(p.parent.parent, seg.withParent(seg.parent.withParent(IrNodePath.empty)) :: acc)

        case seg @ IrNodePathArrayAnyElement(_) =>
          rec(p.parent.parent, seg.withParent(seg.parent.withParent(IrNodePath.empty)) :: acc)

        case seg @ IrNodePathFieldAttribute(_, _) =>
          rec(p.parent.parent, seg.withParent(seg.parent.withParent(IrNodePath.empty)) :: acc)

        case seg @ IrNodePathTextElement(_) =>
          rec(p.parent, seg.withParent(IrNodePath.empty) :: acc)

        case seg @ IrNodePathAnyField(_) =>
          rec(p.parent.parent, seg.withParent(seg.parent.withParent(IrNodePath.empty)) :: acc)
      }
    }

    rec(path, Nil)
  }

  def combine(segments: List[IrNodePath]): IrNodePath =
    segments.foldLeft(IrNodePath.empty)(_ ++ _)

  def renderAsString(path: IrNodePath): String = {
    def rec(irNodePath: IrNodePath, acc: String): String =
      irNodePath match {
        case IrNodePathEmpty if acc.isEmpty =>
          IrNodePathEmpty.name

        case IrNodePathEmpty =>
          if(acc.startsWith(".[")) acc.drop(1) else acc

        case IrNodePathField(fieldName, parentNode) =>
          if(fieldName != MatchIrConstants.unnamedNodeLabel && fieldName != MatchIrConstants.rootNodeLabel)
            rec(parentNode, s".$fieldName$acc")
          else
            rec(parentNode, s".$acc")

        case IrNodePathArrayElement(arrayIndex, parentNode) =>
          rec(parentNode, s"[$arrayIndex]$acc")

        case IrNodePathArrayAnyElement(parentNode) =>
          rec(parentNode, s"[*]$acc")

        case IrNodePathFieldAttribute(attributeName, parentNode) =>
          rec(parentNode, s"['@$attributeName']$acc")

        case IrNodePathTextElement(parentNode) =>
          rec(parentNode, s"['#text']$acc")

        case IrNodePathAnyField(parentNode) =>
          rec(parentNode, s".*$acc")
      }

    rec(path, "")
  }

}
