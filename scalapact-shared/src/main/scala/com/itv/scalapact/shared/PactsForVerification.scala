package com.itv.scalapact.shared

final case class PactsForVerification(_embedded: EmbeddedPactsForVerification, _links: PactsForVerificationLinks) {
  def pacts: List[EmbeddedPactForVerification] = _embedded.pacts
}

final case class EmbeddedPactsForVerification(pacts: List[EmbeddedPactForVerification]) extends AnyVal

final case class PactsForVerificationLinks(self: TitledLink) extends AnyVal

final case class EmbeddedPactForVerification(verificationProperties: VerificationProperties) extends AnyVal {
  def link: String = verificationProperties._links.self.href
  def notices: List[Map[String, String]] = verificationProperties.notices
}

final case class VerificationProperties(notices: List[Map[String, String]], _links: PactSelfLink)

final case class PactSelfLink(self: NamedLink) extends AnyVal

final case class NamedLink(name: String, href: String)

final case class TitledLink(title: String, href: String)