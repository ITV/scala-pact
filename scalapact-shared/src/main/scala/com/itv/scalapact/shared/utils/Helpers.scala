package com.itv.scalapact.shared.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime

import com.itv.scalapact.shared.utils.ColourOutput.ColouredString

import scala.annotation.tailrec
import scala.util.Try
import scala.util.control.NonFatal

object Helpers {

  implicit class TryOps[A](val value: Option[A]) extends AnyVal {
    def whenEmpty(todo: => Unit): Option[A] = value match {
      case None =>
        todo
        None
      case x => x
    }
  }

  def pair[A]: List[A] => Map[A, A] = list => pairTuples(list).foldLeft(Map.empty[A, A])(_ + _)

  def pairTuples[A]: List[A] => List[(A, A)] = list => {
    @tailrec
    def rec(l: List[A], acc: List[(A, A)]): List[(A, A)] =
      l match {
        case Nil         => acc
        case _ :: Nil    => acc
        case x :: y :: _ => rec(l.drop(2), (x, y) :: acc)
      }

    rec(list, Nil).foldLeft(List[(A, A)]())((a, b) => b :: a)
  }

  // Maybe negative, must have digits, may have decimal and if so must have a
  // digit after it, can have more trailing digits.
  val isNumericValueRegex = """^-?\d+\.?\d*$"""
  val isBooleanValueRegex = """^(true|false)$"""

  def safeStringToInt(str: String): Option[Int] =
    Try(str.toInt).toOption.whenEmpty(
      PactLogger.error(s"Failed to convert string '$str' to number (int)".red)
    )
  def safeStringToLong(str: String): Option[Long] = Try(str.toLong).toOption.whenEmpty(
    PactLogger.error(s"Failed to convert string '$str' to number (long)".red)
  )
  def safeStringToBoolean(str: String): Option[Boolean] = Try(str.toBoolean).toOption.whenEmpty(
    PactLogger.error(s"Failed to convert string '$str' to boolean".red)
  )
  def safeStringToDouble(str: String): Option[Double] = Try(str.toDouble).toOption.whenEmpty(
    PactLogger.error(s"Failed to convert string '$str' to number (double)".red)
  )

  def safeStringToDateTime(str: String): Option[OffsetDateTime] = Try(OffsetDateTime.parse(str)).toOption.whenEmpty(
    PactLogger.error(s"Failed to convert string '$str' to date (OffsetDateTime).".red)
  )

  val urlEncode: String => Either[String, String] = str => {
    try Right(
      URLEncoder
        .encode(str, StandardCharsets.UTF_8.toString)
        .replace("+", "%20")
        .replace("%21", "!")
        .replace("%27", "'")
        .replace("%28", "(")
        .replace("%29", ")")
        .replace("%7E", "~")
    )
    catch {
      case NonFatal(_) =>
        Left(s"Failed to url encode: $str")
    }
  }

}
