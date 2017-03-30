package com.itv.scalapactcore.common.matchir

import scala.xml.{Elem, XML, Node}

import com.itv.scalapactcore.common.ColourOuput._

import argonaut._

object MatchIR extends XmlConversionFunctions with JsonConversionFunctions with PrimitiveConversionFunctions {

  def fromXml(xmlString: String): Option[IrNode] =
    safeStringToXml(xmlString).map { elem =>
      nodeToIrNode(elem)
    }

  def fromJSON(jsonString: String): Option[IrNode] =
    Parse.parseOption(jsonString).flatMap { json =>
      jsonRootToIrNode(json)
    }

}

trait JsonConversionFunctions {

  protected def jsonToIrNode(label: String, json: Json): IrNode = {
    json match {
      case j: Json if j.isArray =>
        IrNode(label, None, Map(), None, jsonArrayToIrNodeList(label, j))

      case j: Json if j.isObject =>
        IrNode(label, None, Map(), None, jsonObjectToIrNodeList(j))

      case j: Json if j.isNull =>
        IrNode(label, None, Map(), Some(null), Nil)

      case j: Json if j.isNumber =>
        IrNode(label, None, Map(), j.number.flatMap(_.toDouble).map(d => IrNumberNode(d)), Nil)

      case j: Json if j.isBool =>
        IrNode(label, None, Map(), j.bool.map(IrBooleanNode), Nil)

      case j: Json if j.isString =>
        IrNode(label, None, Map(), j.string.map(IrStringNode), Nil)
    }

  }

  protected def jsonObjectToIrNodeList(json: Json): List[IrNode] =
    json.objectFieldsOrEmpty.map(p => json.field(p).map(q => jsonToIrNode(p, q))).collect { case Some(s) => s }

  protected def jsonArrayToIrNodeList(parentLabel: String, json: Json): List[IrNode] = {
    json.arrayOrEmpty.map(j => jsonToIrNode(parentLabel, j))
  }

  protected def jsonRootToIrNode(json: Json): Option[IrNode] =
    json match {
      case j: Json if j.isArray =>
        Option(
          IrNode("", None, Map(), None, jsonArrayToIrNodeList("", j))
        )

      case j: Json if j.isObject =>
        Option(
          IrNode("", None, Map(), None, jsonObjectToIrNodeList(j))
        )

      case _ =>
        println("JSON was not an object or an array".red)
        None
    }

}

trait XmlConversionFunctions extends PrimitiveConversionFunctions {

  protected def convertAttributes(attributes: Map[String, String]): Map[String, IrNodePrimitive] =
    attributes.flatMap {
      case (k, v) if v == null => Map(k -> IrNullNode)
      case (k, v) if v.matches(isNumericValueRegex) => safeStringToDouble(v).map(IrNumberNode).map(vv => Map(k -> vv)).getOrElse(Map.empty[String, IrNumberNode])
      case (k, v) if v.matches(isBooleanValueRegex) => safeStringToBoolean(v).map(IrBooleanNode).map(vv => Map(k -> vv)).getOrElse(Map.empty[String, IrBooleanNode])
      case (k, v) => Map(k -> IrStringNode(v))
    }

  protected def childNodesToValueMaybePrimitive(nodes: List[Node], value: String): Option[IrNodePrimitive] =
    nodes match {
      case Nil if value == null => Option(IrNullNode)
      case Nil if value.isEmpty => None
      case Nil if value.matches(isNumericValueRegex) => safeStringToDouble(value).map(IrNumberNode)
      case Nil if value.matches(isBooleanValueRegex) => safeStringToBoolean(value).map(IrBooleanNode)
      case Nil => Option(IrStringNode(value))
      case _ => None
    }

  protected def extractNodeValue(node: Node): Option[IrNodePrimitive] =
    childNodesToValueMaybePrimitive(node.child.flatMap(_.child).toList, node.child.text)

  protected def extractNodeChildren(node: Node): List[IrNode] =
    node.child.toList.map(nodeToIrNode)

  protected def nodeToIrNode(node: Node): IrNode =
    extractNodeValue(node) match {
      case nodeValue: Some[IrNodePrimitive] =>
        IrNode(node.label, Option(node.prefix), convertAttributes(node.attributes.asAttrMap), nodeValue, Nil)

      case None =>
        IrNode(node.label, Option(node.prefix), convertAttributes(node.attributes.asAttrMap), None, extractNodeChildren(node))
    }

}

trait PrimitiveConversionFunctions {

  // Maybe negative, must have digits, may have decimal and if so must have a
  // digit after it, can have more trailing digits.
  val isNumericValueRegex = """^-?\d+\.?\d*$"""
  val isBooleanValueRegex = """^(true|false)$"""

  def safeStringToDouble(str: String): Option[Double] =
    try {
      Option(str.toDouble)
    } catch {
      case _: Throwable =>
        println(s"Failed to convert string '$str' to number (double)".red)
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

}

case class IrNode(label: String, ns: Option[String], attributes: Map[String, IrNodePrimitive], value: Option[IrNodePrimitive], children: List[IrNode]) {

  def renderAsString(indent: Int = 0): String = {
    val i = List.fill(indent)("  ").mkString
    val n = ns.map(" " + _ + "").getOrElse("")
    val v = value.map(" " + _.toString()).getOrElse("")
    val a = if(attributes.isEmpty) "" else s"(${attributes.map(p => p._1 + "=" + p._2.toString()).mkString(", ")})"
    val c = if(children.isEmpty) "" else "\n" + children.map(_.renderAsString(indent + 1)).mkString("\n")
    s"$i- $label$n$v$a$c"
  }

}

sealed trait IrNodePrimitive {
  def isString: Boolean
  def isNumber: Boolean
  def isBoolean: Boolean
  def isNull: Boolean
  def asString: Option[String]
  def asNumber: Option[Double]
  def asBoolean: Option[Boolean]
  override def toString(): String
}
case class IrStringNode(value: String) extends IrNodePrimitive {
  def isString: Boolean = true
  def isNumber: Boolean = false
  def isBoolean: Boolean = false
  def isNull: Boolean = false
  def asString: Option[String] = Option(value)
  def asNumber: Option[Double] = None
  def asBoolean: Option[Boolean] = None
  override def toString(): String = value.toString
}
case class IrNumberNode(value: Double) extends IrNodePrimitive {
  def isString: Boolean = false
  def isNumber: Boolean = true
  def isBoolean: Boolean = false
  def isNull: Boolean = false
  def asString: Option[String] = None
  def asNumber: Option[Double] = Option(value)
  def asBoolean: Option[Boolean] = None
  override def toString(): String = value.toString
}
case class IrBooleanNode(value: Boolean) extends IrNodePrimitive {
  def isString: Boolean = false
  def isNumber: Boolean = false
  def isBoolean: Boolean = true
  def isNull: Boolean = false
  def asString: Option[String] = None
  def asNumber: Option[Double] = None
  def asBoolean: Option[Boolean] = Option(value)
  override def toString(): String = value.toString
}
case object IrNullNode extends IrNodePrimitive {
  def isString: Boolean = false
  def isNumber: Boolean = false
  def isBoolean: Boolean = false
  def isNull: Boolean = true
  def asString: Option[String] = None
  def asNumber: Option[Double] = None
  def asBoolean: Option[Boolean] = None
  override def toString(): String = "null"
}
