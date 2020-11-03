package com.itv.scalapact.model

import com.itv.scalapact.shared.http.SslContextMap
import com.itv.scalapact.shared.utils.Maps._

final case class ScalaPactDescription(
    strict: Boolean,
    consumer: String,
    provider: String,
    sslContextName: Option[String],
    interactions: List[ScalaPactInteraction]
) {

  /** Adds interactions to the Pact. Interactions should be created using the helper object 'interaction'
    *
    * @param interaction [ScalaPactInteraction] definition
    * @return [ScalaPactDescription] to allow the builder to continue
    */
  def addInteraction(interaction: ScalaPactInteraction): ScalaPactDescription =
    ScalaPactDescription(strict, consumer, provider, sslContextName, interactions :+ interaction)

  def addSslContextForServer(name: String): ScalaPactDescription =
    ScalaPactDescription(strict, consumer, provider, Some(name), interactions)
}

final case class ScalaPactDescriptionFinal private[scalapact] (
    consumer: String,
    provider: String,
    serverSslContextName: Option[String],
    interactions: List[ScalaPactInteractionFinal],
    options: ScalaPactOptions
) {
  def withHeaderForSsl: ScalaPactDescriptionFinal =
    copy(
      interactions = interactions.map(i =>
        i.copy(
          request = i.request
            .copy(headers = i.request.headers addOpt (SslContextMap.sslContextHeaderName -> i.sslContextName))
        )
      )
    )
}
