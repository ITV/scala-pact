package com.itv.scalapact.shared

object Maps {

  implicit class MapPimper[K, V](map: Map[K, V]) {
    def addOpt(tuple: ( K, Option[V])) = tuple._2.fold(map)(actual => map + (tuple._1 -> actual))
  }

}
