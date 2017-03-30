package com.itv.scalapactcore.common.matchir

import scala.xml.{Elem, XML}

import com.itv.scalapactcore.common.ColourOuput._

import argonaut._

object MatchIR {
  // Maybe negative, must have digits, may have decimal and if so must have a
  // digit after it, can have more trailing digits.
  val isNumericValueRegex = """-?\d*\.?\d*$"""
  val isBooleanValueRegex = """true|false"""

  private def cata[A](str: String, default: A, f: String => A, typeString: String): A =
    try {
      f(str)
    } catch {
      case _: Throwable =>
        println(s"Failed to convert string '$str' to $typeString".red)
        default
    }

  def safeStringToInt(str: String): Option[Int] =
    try {
      Option(str.toInt)
    } catch {
      case _: Throwable =>
        println(s"Failed to convert string '$str' to int".red)
        None
    }

  def safeStringToBoolean(str: String): Option[Boolean] =
    try {
      Option(str.toBoolean)
    } catch {
      case _: Throwable =>
        println(s"Failed to convert string '$str' to boolean".red)
        None
    }

  def safeStringToXml(str: String): Option[Elem] =
    try {
      Option(XML.loadString(str))
    } catch {
      case _: Throwable =>
        println(s"Failed to convert string '$str' to xml".red)
        None
    }

  private def convertAttributes(attributes: Map[String, String]): Map[String, IrNodePrimitive] =
    attributes.flatMap {
      case (k, v) if v == null => Map(k -> IrNullNode)
      case (k, v) if v.matches(isNumericValueRegex) => safeStringToInt(v).map(IrIntNode).map(vv => Map(k -> vv)).getOrElse(Map.empty[String, IrIntNode])
      case (k, v) if v.matches(isBooleanValueRegex) => safeStringToBoolean(v).map(IrBooleanNode).map(vv => Map(k -> vv)).getOrElse(Map.empty[String, IrBooleanNode])
      case (k, v) => Map(k -> IrStringNode(v))
    }

  private def extractElemValue(elem: Elem): Option[IrNodePrimitive] = {
    val value: String = elem.child.text

    elem.child.flatMap(_.child).toList match {
      case Nil if value == null => Option(IrNullNode)
      case Nil if value.matches(isNumericValueRegex) => safeStringToInt(value).map(IrIntNode)
      case Nil if value.matches(isBooleanValueRegex) => safeStringToBoolean(value).map(IrBooleanNode)
      case Nil => Option(IrStringNode(value))
      case _ => None
    }
  }

  def fromXml(xmlString: String): Option[IrNode] = {

    safeStringToXml(xmlString).map { elem =>

//      println("1> " + elem)
//      println("2> " + elem.label)
//      println("3> " + elem.child)
//      println("4> " + elem.text)
//      println("5> " + elem.isAtom)
//      println("6> " + elem)

      IrNode(elem.label, Option(elem.prefix), convertAttributes(elem.attributes.asAttrMap), extractElemValue(elem), Nil)

//
//      val v: Option[Map[MatchIRLabel, MatchIRAny]] = Option(
//        Map(
//          MatchIRLabel(elem.label) -> Map()
//        )
//      )
//
//      MatchIR(
//        MatchIRRoot(
//          v,
//          None,
//          None
//        )
//      )
    }
  }

  def fromJSON(jsonString: String): Option[IrNode] = {


    throw new Exception()
//
//    Parse.parseOption(jsonString).map {
//      case j: JObject => ???
//      case j: JArray => ???
//    }
//
//    MatchIR(MatchIRRoot(None, None, None))
  }

}

case class IrNode(label: String, ns: Option[String], attributes: Map[String, IrNodePrimitive], value: Option[IrNodePrimitive], children: List[IrNode])

sealed trait IrNodePrimitive {
  def isString: Boolean
  def isInt: Boolean
  def isBoolean: Boolean
  def isNull: Boolean
  def asString: Option[String]
  def asInt: Option[Int]
  def asBoolean: Option[Boolean]
}
case class IrStringNode(value: String) extends IrNodePrimitive {
  def isString: Boolean = true
  def isInt: Boolean = false
  def isBoolean: Boolean = false
  def isNull: Boolean = false
  def asString: Option[String] = Option(value)
  def asInt: Option[Int] = None
  def asBoolean: Option[Boolean] = None
}
case class IrIntNode(value: Int) extends IrNodePrimitive {
  def isString: Boolean = false
  def isInt: Boolean = true
  def isBoolean: Boolean = false
  def isNull: Boolean = false
  def asString: Option[String] = None
  def asInt: Option[Int] = Option(value)
  def asBoolean: Option[Boolean] = None
}
case class IrBooleanNode(value: Boolean) extends IrNodePrimitive {
  def isString: Boolean = false
  def isInt: Boolean = false
  def isBoolean: Boolean = true
  def isNull: Boolean = false
  def asString: Option[String] = None
  def asInt: Option[Int] = None
  def asBoolean: Option[Boolean] = Option(value)
}
case object IrNullNode extends IrNodePrimitive {
  def isString: Boolean = false
  def isInt: Boolean = false
  def isBoolean: Boolean = false
  def isNull: Boolean = true
  def asString: Option[String] = None
  def asInt: Option[Int] = None
  def asBoolean: Option[Boolean] = None
}

/*
 root can be either a JSON object, a JSON array, or an XML node
 */
//case class MatchIR(root: MatchIRRoot)
//
//case class MatchIRRoot(map: Option[Map[MatchIRLabel, MatchIRAny]], list: Option[List[MatchIRAny]], field: Option[MatchIRMap])
//
//sealed trait MatchIRAny {
//  val isPrimitive: Boolean
//  val isField: Boolean
//}
//
//sealed trait MatchIRField extends MatchIRAny {
//  val label: MatchIRLabel
//  val isPrimitive: Boolean = false
//  val isField: Boolean = true
//}
//case class MatchIRMap(label: MatchIRLabel, fields: Map[MatchIRLabel, MatchIRAny]) extends MatchIRField
//case class MatchIRList(label: MatchIRLabel, children: List[MatchIRAny]) extends MatchIRField
//
//case class MatchIRLabel(value: String)
//
//// Primitives
//sealed trait MatchIRPrimitive extends MatchIRAny {
//  val isPrimitive: Boolean = true
//  val isField: Boolean = false
//}
//case class MatchIRString(value: String) extends MatchIRPrimitive
//case class MatchIRInt(value: String) extends MatchIRPrimitive
//case class MatchIRBoolean(value: String) extends MatchIRPrimitive

/*

JSON contains:
object
array
string
int
boolean
null

XML contains
nodes with child nodes
nodes with attributes
untyped node values

You can model JSON in XML but not the other way around in either direction you loose information.
XML to JSON you loose attributes
JSON to XML you loose some type information e.g. XML has no explicit definition of arrays

Some cases:

#1
{
  "fish":{}
}
equals
<fish></fish>

 */