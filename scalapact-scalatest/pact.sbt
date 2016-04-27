import com.itv.scalapact.plugin.ScalaPactPlugin._

providerStates := Seq(
  ("Resource with ID 1234 exists", (key: String) => {
    println("Injecting key 1234 into the database...")
    // Do some work to ensure the system under test is
    // in an appropriate state before verification
    true
  })
)

pactBrokerAddress := "http://pactbroker.infradev.oasvc.itv.com"
providerName := "Their Provider Service"
consumerNames := Seq("My Consumer")
pactContractVersion := "1.0.0"