package com.itv.scalapact.shared

import com.itv.scalapact.shared.Pact.Links

/*
{
  "_embedded": {
    "pacts": [
      {
        "verificationProperties": {
          "notices": [
            {
              "text": "This pact is being verified because it is the pact for the latest version of Foo tagged with 'dev'"
            }
          ],
        },
        "_links": {
          "self": {
            "href": "http://localhost:9292/pacts/provider/Bar/consumer/Foo/pact-version/0e3369199f4008231946e0245474537443ccda2a",
            "name": "Pact between Foo (v1.0.0) and Bar"
          }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost:9292/pacts/provider/Bar/for-verification",
      "title": "Pacts to be verified"
    }
  }
}
*/


final case class PactsForVerificationResponse(_embedded: EmbeddedPactsForVerification, _links: Links) {
  def pacts: List[EmbeddedPactForVerification] = _embedded.pacts
}

final case class EmbeddedPactsForVerification(pacts: List[EmbeddedPactForVerification]) extends AnyVal

final case class EmbeddedPactForVerification(verificationProperties: VerificationProperties, _links: Links) {
  def href: Option[String] = _links.get("self").map(_.href)
  def notices: List[Map[String, String]] = verificationProperties.notices
}

final case class VerificationProperties(notices: List[Map[String, String]]) extends AnyVal
