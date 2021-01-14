import com.itv.scalapact.shared.ConsumerVersionSelector
import scala.concurrent.duration._

pactBrokerAddress := "https://test.pact.dius.com.au"
//For publishing to pact-broker test server (these credentials are public knowledge)
pactBrokerCredentials := ("dXfltyFMgNOFZAxr8io9wJ37iUpY42M", "O5AIZWxelWbLvqMd8PkAVycBJh2Psyg1")
consumerVersionSelectors := Seq(ConsumerVersionSelector("test", latest = true))
providerVersionTags := List("master")
providerName := "scala-pact-integration-test-provider"
pactBrokerClientTimeout := 5.seconds