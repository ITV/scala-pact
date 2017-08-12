package com.itv.scalapactcore.common.matchir

sealed trait IrNodePath {
  def <~(fieldName: String): IrNodePath =
    if(fieldName == "*") IrNodePathArrayAnyElement(this) else IrNodePathField(fieldName, this)

  def <~(arrayIndex: Int): IrNodePath = IrNodePathArrayElement(arrayIndex, this)

  def renderAsString: String = {
    def rec(irNodePath: IrNodePath, acc: String): String = {
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
