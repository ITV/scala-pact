package com.itv.scalapactcore.common.matchir

import scala.xml.{Elem, XML, Node}

import com.itv.scalapactcore.common.ColourOuput._

import argonaut._

object MatchIrConverters extends XmlConversionFunctions with JsonConversionFunctions with PrimitiveConversionFunctions {

  implicit def xmlToIrNode(elem: Elem): IrNode =
    nodeToIrNode(scala.xml.Utility.trim(elem), IrNodePathEmpty)


  implicit def jsonToIrNode(json: Json): IrNode =
    jsonRootToIrNode(json, IrNodePathEmpty) match {
      case Some(v) => v
      case None => IrNode.empty
    }
}

object MatchIr extends XmlConversionFunctions with JsonConversionFunctions with PrimitiveConversionFunctions {

  def fromXml(xmlString: String): Option[IrNode] =
    safeStringToXml(xmlString).map { elem =>
      nodeToIrNode(scala.xml.Utility.trim(elem), IrNodePathEmpty)
    }

  def fromJSON(jsonString: String): Option[IrNode] =
    Parse.parseOption(jsonString).flatMap { json =>
      jsonRootToIrNode(json, IrNodePathEmpty)
    }

}

trait JsonConversionFunctions {

  val rootNodeLabel = "(--root node--)"
  val unnamedNodeLabel = "(--node has no label--)"

  protected def jsonToIrNode(label: String, json: Json, pathToParent: IrNodePath): IrNode = {
    json match {
      case j: Json if j.isArray =>
        IrNode(label, jsonArrayToIrNodeList(label, j, pathToParent)).withPath(pathToParent)
          .markAsArray

      case j: Json if j.isObject =>
        IrNode(label, jsonObjectToIrNodeList(j, pathToParent)).withPath(pathToParent)

      case j: Json if j.isNull =>
        IrNode(label, IrNullNode).withPath(pathToParent)

      case j: Json if j.isNumber =>
        IrNode(label, j.number.flatMap(_.toDouble).map(d => IrNumberNode(d))).withPath(pathToParent)

      case j: Json if j.isBool =>
        IrNode(label, j.bool.map(IrBooleanNode)).withPath(pathToParent)

      case j: Json if j.isString =>
        IrNode(label, j.string.map(IrStringNode)).withPath(pathToParent)
    }

  }

  protected def jsonObjectToIrNodeList(json: Json, pathToParent: IrNodePath): List[IrNode] = {
    json
      .objectFieldsOrEmpty
      .map(l => if (l.isEmpty) unnamedNodeLabel else l)
      .map { l =>
        json.field(l).map {
          q => jsonToIrNode(l, q, pathToParent <~ l)
        }
      }
      .collect { case Some(s) => s }
  }

  protected def jsonArrayToIrNodeList(parentLabel: String, json: Json, pathToParent: IrNodePath): List[IrNode] = {
    json.arrayOrEmpty.zipWithIndex
      .map(j => jsonToIrNode(parentLabel, j._1, pathToParent <~ j._2))
  }

  protected def jsonRootToIrNode(json: Json, initialPath: IrNodePath): Option[IrNode] =
    json match {
      case j: Json if j.isArray =>
        Option(
          IrNode(rootNodeLabel, jsonArrayToIrNodeList(unnamedNodeLabel, j, initialPath))
            .withPath(initialPath)
            .markAsArray
        )

      case j: Json if j.isObject =>
        Option(
          IrNode(rootNodeLabel, jsonObjectToIrNodeList(j, initialPath))
            .withPath(initialPath)
        )

      case _ =>
        println("JSON was not an object or an array".red)
        None
    }

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
        None
    }

}