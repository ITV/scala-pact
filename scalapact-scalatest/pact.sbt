// import com.itv.scalapact.plugin.ScalaPactPlugin._
//
// providerStates := Seq(
//   ("Resource with ID 1234 exists", (_: String) => {
//     println("Injecting key 1234 into the database...")
//     // Do some work to ensure the system under test is
//     // in an appropriate state before verification
//
//     true
//   })
// )
//
// providerStateMatcher := {
//   case key if key == "Resource with ID 1234 exists" =>
//     println("Injecting key 1234 into the database...")
//     true
// }
//
// pactBrokerAddress := "http://localhost"
// providerName := "Their Provider Service"
// consumerNames := Seq("My Consumer")
// pactContractVersion := "2.0.0"
// allowSnapshotPublish := false
