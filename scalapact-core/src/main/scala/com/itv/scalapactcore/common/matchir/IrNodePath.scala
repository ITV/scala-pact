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

  val toPactPath: IrNodePath => String = _.renderAsString

  private val fieldCapture: Regex = """^(.|\[[\'|\"])([a-zA-Z0-9:-_]+)([\'|\"]\])?""".r

  val fromPactPath: String => Option[IrNodePath] = pactPath => {
    def rec(remaining: String): Option[IrNodePath] =
      remaining match {
        case fieldCapture(_, fieldName, _) =>
          println(fieldName)
          None

        case _ =>
          println("Boom!")
          None

      }

    rec(pactPath)
  }

}

sealed trait IrNodePath {
  def <~(fieldName: String): IrNodePath =
    if(fieldName == "*") IrNodePathArrayAnyElement(this) else IrNodePathField(fieldName, this)

  def <~(arrayIndex: Int): IrNodePath = IrNodePathArrayElement(arrayIndex, this)

  def <@(attributeName: String): IrNodePath = IrNodePathFieldAttribute(attributeName, this)

  def text: IrNodePath = IrNodePathTextElement(this)

  def ===(other: IrNodePath): Boolean = {
    @tailrec
    def rec(a: IrNodePath, b: IrNodePath): Boolean =
      (a, b) match {
        case (IrNodePathEmpty, IrNodePathEmpty) =>
          true

        case (IrNodePathField(fieldNameA, parentA), IrNodePathField(fieldNameB, parentB)) if fieldNameA == fieldNameB =>
          rec(parentA, parentB)

        case (IrNodePathArrayElement(indexA, parentA), IrNodePathArrayElement(indexB, parentB)) if indexA == indexB =>
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
          acc

        case IrNodePathField(fieldName, parent) =>
          rec(parent, s".$fieldName$acc")

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
