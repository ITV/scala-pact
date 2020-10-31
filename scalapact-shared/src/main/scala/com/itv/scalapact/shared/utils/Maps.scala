package com.itv.scalapact.shared.utils

object Maps {

  implicit class MapPimper[K, V](map: Map[K, V]) {
    def addOpt(tuple: (K, Option[V])): Map[K, V] =
      tuple._2.fold(map)(actual => map + (tuple._1 -> actual))
  }

}
