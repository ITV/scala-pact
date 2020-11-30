import com.itv.scalapact.shared.ConsumerVersionSelector

pactBrokerAddress := "https://test.pact.dius.com.au"
//For publishing to pact-broker test server (these credentials are public knowledge)
pactBrokerCredentials := ("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1")
consumerVersionSelectors := Seq(ConsumerVersionSelector("test", latest = true))
providerVersionTags := List("master")
providerName := "scala-pact-pending-test-provider"