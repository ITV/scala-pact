package com.itv.scalapact.tapir

object BodyMatching {

  def hasXmlHeader(headers: Option[Map[String, String]]): Boolean =
    findContentTypeHeader(headers).exists(_.toLowerCase.contains("xml"))

  def hasJsonHeader(headers: Option[Map[String, String]]): Boolean =
    findContentTypeHeader(headers).exists(_.toLowerCase.contains("json"))

  def findContentTypeHeader(headers: Option[Map[String, String]]): Option[String] =
    headers
      .map { hm =>
        hm.find(p => p._1.toLowerCase == "content-type").map(_._2)
      }
      .toList
      .headOption
      .flatten

  lazy val stringIsProbablyJson: String => Boolean = str =>
    ((s: String) => s.nonEmpty && ((s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]"))))(
      str.trim
    )

  lazy val stringIsProbablyXml: String => Boolean = str =>
    ((s: String) => s.nonEmpty && s.startsWith("<") && s.endsWith(">"))(str.trim)

}
