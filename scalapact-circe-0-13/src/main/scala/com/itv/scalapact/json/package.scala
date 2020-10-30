package com.itv.scalapact

import com.itv.scalapact.circe13.JsonInstances

package object json extends JsonInstances {
  val JsonConversionFunctions: circe13.JsonConversionFunctions.type = circe13.JsonConversionFunctions
}
