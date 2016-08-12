import com.itv.scalapact.plugin.ScalaPactPlugin._

providerStates := Seq(
  ("Results: Bob, Fred, Harry", (key: String) => {
    // Do some work to ensure the system under test is
    // in an appropriate state before verification
    true
  })
)
