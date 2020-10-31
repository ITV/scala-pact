package com.itv.scalapact.shared

import com.itv.scalapact.shared.Notice.{PendingStateNotice, SimpleNotice}

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
  def href: Option[String] = _links.get("self").map(_.href)
}

sealed trait VerificationProperties {
  type A <: Notice
  def pending: Boolean
  def notices: List[A]
}

object VerificationProperties {
  sealed trait VerificationPropertiesAux[A0 <: Notice] extends VerificationProperties { type A = A0 }

  final case class SimpleVerificationProperties(notices: List[SimpleNotice])
      extends VerificationPropertiesAux[SimpleNotice] {
    val pending = false
  }

  final case class PendingStateVerificationProperties(pending: Boolean, notices: List[PendingStateNotice])
      extends VerificationPropertiesAux[PendingStateNotice]

}

sealed trait Notice {
  def text: String
}

object Notice {
  final case class SimpleNotice(text: String)             extends Notice
  sealed trait PendingStateNotice                         extends Notice
  final case class BeforeVerificationNotice(text: String) extends PendingStateNotice
  final case class AfterVerificationNotice(text: String, success: Boolean, published: Boolean)
      extends PendingStateNotice
}
