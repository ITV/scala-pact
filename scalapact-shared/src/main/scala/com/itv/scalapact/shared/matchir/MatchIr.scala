package com.itv.scalapact.shared.matchir

import scala.xml.{Elem, Node, XML}
import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared.PactLogger

object MatchIr extends XmlConversionFunctions with PrimitiveConversionFunctions {

  def fromXmlString(xmlString: String): Option[IrNode] =
    safeStringToXml(xmlString).flatMap(fromXml)

  def fromXml(xml: Elem): Option[IrNode] =
    Option {
      nodeToIrNode(scala.xml.Utility.trim(xml), IrNodePathEmpty)
    }

  def fromJSON(fromJson: String => Option[IrNode])(jsonString: String): Option[IrNode] =
    fromJson(jsonString)

}

trait XmlConversionFunctions extends PrimitiveConversionFunctions {

  protected def convertAttributes(attributes: Map[String, String], pathToParent: IrNodePath): IrNodeAttributes =
    attributes.toList.reverse.map {
      case (k, v) if v == null =>
        IrNodeAttributes(Map(k -> IrNodeAttribute(IrNullNode, pathToParent <@ k)))

      case (k, v) if v.matches(isNumericValueRegex) =>
        safeStringToDouble(v)
          .map(IrNumberNode)
          .map(vv => IrNodeAttributes(Map(k -> IrNodeAttribute(vv, pathToParent <@ k))))
          .getOrElse(IrNodeAttributes.empty)

      case (k, v) if v.matches(isBooleanValueRegex) =>
        safeStringToBoolean(v)
          .map(IrBooleanNode)
          .map(vv => IrNodeAttributes(Map(k -> IrNodeAttribute(vv, pathToParent <@ k))))
          .getOrElse(IrNodeAttributes.empty)

      case (k, v) =>
        IrNodeAttributes(Map(k -> IrNodeAttribute(IrStringNode(v), pathToParent <@ k)))

    }.foldLeft(IrNodeAttributes.empty)(_ + _)

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

  protected def extractNodeChildren(node: Node, pathToParent: IrNodePath): List[IrNode] =
    node.child.toList.map(n => nodeToIrNode(n, pathToParent))

  protected def nodeToIrNode(node: Node, initialPath: IrNodePath): IrNode =
    extractNodeValue(node) match {
      case nodeValue: Some[IrNodePrimitive] =>
        IrNode(node.label, nodeValue, Nil, Option(node.prefix), convertAttributes(node.attributes.asAttrMap, initialPath <~ node.label), false, true, initialPath <~ node.label)

      case None =>
        val children = extractNodeChildren(node, initialPath <~ node.label)
        val isArray: Boolean =
          children match {
            case Nil =>
              false

            case x :: _ if children.length > 1 && children.forall(_.label == x.label) =>
              true

            case _ =>
              false
          }

        val updatedChildren =
          if (isArray) children.zipWithIndex.map(ci => ci._1.withPath(ci._1.path <~ ci._2))
          else children

        IrNode(node.label, None, updatedChildren, Option(node.prefix), convertAttributes(node.attributes.asAttrMap, initialPath <~ node.label), isArray, true, initialPath <~ node.label)
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
        PactLogger.error(s"Failed to convert string '$str' to number (double)".red)
        None
    }

  def safeStringToBoolean(str: String): Option[Boolean] =
    try {
      Option(str.toBoolean)
    } catch {
      case _: Throwable =>
        PactLogger.error(s"Failed to convert string '$str' to boolean".red)
        None
    }

  def safeStringToXml(str: String): Option[Elem] =
    try {
      Option(XML.loadString(str))
    } catch {
      case _: Throwable =>
        None
    }

}