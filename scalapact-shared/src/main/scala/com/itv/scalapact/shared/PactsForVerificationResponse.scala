package com.itv.scalapact.shared

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
  def pacts: List[PactForVerification] = _embedded.pacts
}

final case class EmbeddedPactsForVerification(pacts: List[PactForVerification]) extends AnyVal

final case class PactForVerification(verificationProperties: VerificationProperties, _links: Links) {
  def href: Option[String] = _links.get("self").flatMap {
    case LinkValues(_, _, href, _) => Some(href)
    case _: LinkList               => None
  }
}

final case class VerificationProperties(pending: Boolean, notices: List[Notice])

sealed trait Notice {
  def text: String
}

object Notice {
  final case class SimpleNotice(text: String)                                                  extends Notice
  final case class BeforeVerificationNotice(text: String)                                      extends Notice
  final case class AfterVerificationNotice(text: String, success: Boolean, published: Boolean) extends Notice
}
