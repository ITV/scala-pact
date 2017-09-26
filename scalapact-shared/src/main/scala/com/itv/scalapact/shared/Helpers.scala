package com.itv.scalapact.shared

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import scala.util.control.NonFatal

object Helpers {

  val pair: List[String] => Map[String, String] = list => {
    @annotation.tailrec
    def rec[A](l: List[A], acc: List[Map[A, A]]): List[Map[A, A]] = {
      l match {
        case Nil => acc
        case _ :: Nil => acc
        case x :: xs => rec(l.drop(2), Map(x -> xs.head) :: acc)
      }
    }

    rec(list, Nil).foldLeft(Map[String, String]())(_ ++ _)
  }

  val pairTuples: List[String] => List[(String, String)] = list => {
    @annotation.tailrec
    def rec[A](l: List[A], acc: List[(A, A)]): List[(A, A)] = {
      l match {
        case Nil => acc
        case _ :: Nil => acc
        case x :: xs => rec(l.drop(2), (x, xs.head) :: acc)
      }
    }

    rec(list, Nil).foldLeft(List[(String, String)]())((a, b) => b :: a)
  }

  val safeStringToInt: String => Option[Int] = s =>
    try {
      Option(s.toInt)
    } catch {
      case NonFatal(_) =>
        None
    }

  val safeStringToLong: String => Option[Long] = s =>
    try {
      Option(s.toLong)
    } catch {
      case NonFatal(_) =>
        None
    }

  val safeStringToBoolean: String => Option[Boolean] = s =>
    try {
      Option(s.toBoolean)
    } catch {
      case NonFatal(_) =>
        None
    }

  val urlEncode: String => Either[String, String] = str => {
    try {
      Right(
        URLEncoder.encode(str, StandardCharsets.UTF_8.toString)
          .replace("+", "%20")
          .replace("%21", "!")
          .replace("%27", "'")
          .replace("%28", "(")
          .replace("%29", ")")
          .replace("%7E", "~")
      )
    } catch {
      case NonFatal(_) =>
        Left(s"Failed to url encode: $str")
    }
  }

}
