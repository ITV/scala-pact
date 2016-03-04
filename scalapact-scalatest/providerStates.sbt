import com.itv.scalapact.plugin.ScalaPactPlugin._

providerStates := Seq(
  ("runMe", (key: String) => {
    println("This is the captain speaking... the key was: " + key)
    true
  })
)