package com.itv.scalapact.shared

import com.itv.scalapact.shared.matchir.IrNode

trait IJsonConversionFunctions {
  def fromJSON(jsonString: String): Option[IrNode]
}
