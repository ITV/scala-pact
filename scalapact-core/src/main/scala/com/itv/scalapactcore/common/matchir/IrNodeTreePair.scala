package com.itv.scalapactcore.common.matchir

sealed trait IrNodeTreePair {

  def renderAsString: String = renderAsString(0)

  private def renderAsString(indent: Int): String = {
    val i = List.fill(indent)("  ").mkString

    this match {
      case IrNodeTreePairBoth(a, b, children, matched) =>
        val c = if(children.isEmpty) "" else "\n" + children.map(_.renderAsString(indent + 1)).mkString("\n")
        s"$i- (${a.ns.map(_ + ":").getOrElse("")}${a.label}, ${b.ns.map(_ + ":").getOrElse("")}${b.label}) [${if(matched) "POSITION MATCHED" else "MISALIGNED"}]$c"

      case IrNodeTreePairOnlyA(n) =>
        s"$i- (${n.ns.map(_ + ":").getOrElse("")}${n.label}, _)"

      case IrNodeTreePairOnlyB(n) =>
        s"$i- (_, ${n.ns.map(_ + ":").getOrElse("")}${n.label})"
    }
  }

}
case class IrNodeTreePairBoth(a: IrNode, b: IrNode, children: List[IrNodeTreePair], positionsMatched: Boolean) extends IrNodeTreePair
case class IrNodeTreePairOnlyA(a: IrNode) extends IrNodeTreePair
case class IrNodeTreePairOnlyB(b: IrNode) extends IrNodeTreePair

object IrNodeTreePair {

  def combine(a: IrNode, b: IrNode): IrNodeTreePair =
    combineNodes(List(a), List(b)) match {
      case Nil => throw new Exception("Node combine failed: No tree produced.")
      case x :: Nil => x
      case _ => throw new Exception("Node combine failed: Too many trees produced.")
    }

  private def combineNodes(as: List[IrNode], bs: List[IrNode]): List[IrNodeTreePair] = {
    as.zipWithIndex.map { p =>
      fetchFromIndex(p._2, bs)
        .flatMap { n =>
          if(n.label == p._1.label && n.ns == p._1.ns && n.value == p._1.value) Some(n) else None
        }
        .map(b => IrNodeTreePairBoth(p._1, b, combineNodes(p._1.children, b.children), positionsMatched = true)) match {
        case Some(s) => s
        case None =>
          bs
            .find(n => n.label == p._1.label && n.ns == p._1.ns && n.value == p._1.value)
            .map(b => IrNodeTreePairBoth(p._1, b, combineNodes(p._1.children, b.children), positionsMatched = false))
            .getOrElse(IrNodeTreePairOnlyA(p._1))
      }
    }
  }

  private val fetchFromIndex: (Int, List[IrNode]) => Option[IrNode] = (index, list) =>
    try {
      Option(list(index))
    } catch {
      case _: Throwable => None
    }

}