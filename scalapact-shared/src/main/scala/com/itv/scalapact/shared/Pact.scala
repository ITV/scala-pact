package com.itv.scalapact.shared

import com.itv.scalapact.shared.http.SslContextMap

final case class Pact(provider: PactActor,
                consumer: PactActor,
                interactions: List[Interaction],
                _links: Option[Links],
                metadata: Option[PactMetaData]) {

  def withoutSslHeader: Pact = copy(interactions = interactions.map(_.withoutSslHeader))

  def renderAsString: String =
    s"""Pact
       |  consumer: [${consumer.renderAsString}]
       |  provider: [${provider.renderAsString}]
       |  interactions:
       |${interactions.map(_.renderAsString).mkString("\n")}
     """.stripMargin

}

final case class PactActor(name: String) extends AnyVal {

  def renderAsString: String =
    s"""$name"""

}

final case class Interaction(providerState: Option[String],
                       description: String,
                       request: InteractionRequest,
                       response: InteractionResponse) {

  def withoutSslHeader: Interaction =
    copy(request = request.copy(headers = request.headers.map(_ - SslContextMap.sslContextHeaderName)))

  def renderAsString: String =
    s"""Interaction
       |  providerState: [${providerState.getOrElse("<none>")}]
       |  description:   [$description]
       |  ${request.renderAsString}
       |  ${response.renderAsString}
    """.stripMargin

}

final case class InteractionRequest(method: Option[String],
                              path: Option[String],
                              query: Option[String],
                              headers: Option[Map[String, String]],
                              body: Option[String],
                              matchingRules: Option[Map[String, MatchingRule]]) {
  def withoutSslContextHeader: InteractionRequest = copy(headers = headers.map(_ - SslContextMap.sslContextHeaderName))

  def sslContextName: Option[String] = headers.flatMap(_.get(SslContextMap.sslContextHeaderName))

  def renderAsString: String =
    s"""Request           [${method.getOrElse("<missing method>")}]
       |  path:           [${path.getOrElse("<missing path>")}]
       |  query:          [${query.getOrElse("<missing path>")}]
       |  headers:        [${headers
         .map(_.toList.map(p => p._1 + "=" + p._2).mkString(",\n                   "))
         .getOrElse("")}]
       |  matching rules: [${matchingRules
         .map(_.toList.map(p => p._1 + " -> (" + p._2.renderAsString + ")").mkString(",\n                   "))
         .getOrElse("")}]
       |  body:
       |${body.getOrElse("[no body]")}
       |
     """.stripMargin
}

final case class InteractionResponse(status: Option[Int],
                               headers: Option[Map[String, String]],
                               body: Option[String],
                               matchingRules: Option[Map[String, MatchingRule]]) {

  def renderAsString: String =
    s"""Response          [${status.map(_.toString).getOrElse("<missing status>")}]
       |  headers:        [${headers
         .map(_.toList.map(p => p._1 + "=" + p._2).mkString(",\\n                   \""))
         .getOrElse("")}]
       |  matching rules: [${matchingRules
         .map(_.toList.map(p => p._1 + " -> (" + p._2.renderAsString + ")").mkString(",\n                   "))
         .getOrElse("")}]
       |  body:
       |${body.getOrElse("[no body]")}
       |
     """.stripMargin

}

final case class MatchingRule(`match`: Option[String], regex: Option[String], min: Option[Int]) {
  def renderAsString: String =
    s"Rule type: '${`match`.getOrElse("<missing>")}'  regex: '${regex.getOrElse("n/a")}'  min: '${min.map(_.toString).getOrElse("n/a")}'"
}

/*
"metadata": {
        "pactSpecification": {
            "version": "2.0.0"
        },
        "scala-pact": {
            "version": <current scala-pact version>
        }
    }
 */
final case class PactMetaData(pactSpecification: Option[VersionMetaData], `scala-pact`: Option[VersionMetaData])
final case class VersionMetaData(version: String) extends AnyVal