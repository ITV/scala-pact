package com.itv.scalapact.shared

sealed trait Link

final case class LinkValues(title: Option[String], name: Option[String], href: String, templated: Option[Boolean])
    extends Link

final case class LinkList(links: List[LinkValues]) extends Link
