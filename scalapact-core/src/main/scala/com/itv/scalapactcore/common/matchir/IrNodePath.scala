package com.itv.scalapactcore.common.matchir

import scala.annotation.tailrec

object IrNodePath {

  val fromJsonPath: String => IrNodePath = ???

  val toJsonPath: IrNodePath => String = ???

  val fromXmlPath: String => IrNodePath = ???

  val toXmlPath: IrNodePath => String = ???

}

sealed trait IrNodePath {
  def <~(fieldName: String): IrNodePath =
    if(fieldName == "*") IrNodePathArrayAnyElement(this) else IrNodePathField(fieldName, this)

  def <~(arrayIndex: Int): IrNodePath = IrNodePathArrayElement(arrayIndex, this)

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

        case (IrNodePathArrayElement(_, parentA), IrNodePathArrayAnyElement(parentB)) =>
          rec(parentA, parentB)

        case _ =>
          false
      }

    rec(this, other)
  }

  val toJsonPath: String = IrNodePath.toJsonPath(this)

  val toXmlPath: String = IrNodePath.toXmlPath(this)

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
