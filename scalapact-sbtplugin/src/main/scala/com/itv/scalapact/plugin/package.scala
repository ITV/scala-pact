package com.itv.scalapact

import com.itv.scalapact.plugin.publish.PublishDetails
import com.itv.scalapactcore.Pact

import scalaj.http.{HttpRequest, HttpResponse}


package object plugin {

  type HttpService = HttpRequest => HttpResponse[String]
  type PublishAddressGenerator = PublishDetails => String
  type PactFormatter = Pact => String
  type PactParser = String => Option[Pact]

  type PactFileContentAggregator = List[Pact] => String
}
