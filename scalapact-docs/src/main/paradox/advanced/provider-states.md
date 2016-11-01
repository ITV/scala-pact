
# Provider states
Scala-Pact currently offers limited support for provider states.

Sometimes, you need to warn your provider that a contract relies on the providing system being in a particular state. For example, your contract describes requesting a document resource via a GET request with the document's id as a query parameter. If you send the Pact contract to your provider, but that document id doesn't exist on their system, then verification will fail through no-ones fault.

To warn the provider about such requirements, we use provider states. In the tests these are simply strings in the `given()` method as below:

```
forgePact
  .between("My Consumer")
  .and("Their Provider Service")
  .addInteraction(
    interaction
      .description("Fetching a specific ID")
      .given("Resource with ID 1234 exists")
      .uponReceiving("/document/lookup?id=1234")
      .willRespondWith(200, "ID: 1234 Exists")
  )
  .runConsumerTest { mockConfig =>

    val result = SimpleClient.doGetRequest(mockConfig.baseUrl, endPoint, Map.empty)

    result.status should equal(200)
    result.body should equal("ID: 1234 Exists")

  }
```
[Example taken from the ExampleSpec test suite.](https://github.com/ITV/scalapact/blob/master/scalapact-scalatest/src/test/scala/com/itv/scalapact/ExampleSpec.scala)

The `given("Resource with ID 1234 exists")` string is actually a key that the provider can hook into! It shows up in the Pact contract under the `providerState` field like this:

```
{
  "provider" : {
    "name" : "Their Provider Service"
  },
  "consumer" : {
    "name" : "My Consumer"
  },
  "interactions" : [
    {
      "providerState" : "Resource with ID 1234 exists",
      "description" : "Fetching a specific ID",
      "request" : {
        "method" : "GET",
        "path" : "/provider-state",
        "query" : "id=1234"
      },
      "response" : {
        "status" : 200,
        "body" : "ID: 1234 Exists"
      }
    }
  ]
}
```

On the provider's side, before verification they can opt to take action on any of these keys by adding a `pact.sbt` file to the root of their Scala project. Here is an example of the contents, again taken from the example suite:

```
import com.itv.scalapact.plugin.ScalaPactPlugin._

providerStates := Seq(
  ("Resource with ID 1234 exists", (key: String) => {
    println("Injecting key 1234 into the database...")
    // Do some work to ensure the system under test is
    // in an appropriate state before verification
    true
  })
)
```

*Notice it's the same string!*

Now when the provider runs the verification, the function they wrote will be invoked prior to the relevant interaction being verified.

The providerStates settings object is a `Seq[(String, String => Boolean)]` where the first string is the `key` and the `String => Boolean` is a function you describe that simply takes the `key` and returns whether your code ran successfully or not.

The intention is to create helper objects in the future for running common tasks like executing shell scripts but at the moment the functions are pure Scala and it's up to you how you use them.
