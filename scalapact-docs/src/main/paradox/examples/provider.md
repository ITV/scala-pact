# The Provider
In the @ref:[consumer](consumer.md) example guide we generated our Pact contract. Now to see if it works!

As previously mentioned, there are two [verification strategies](../articles/verification-strategies.md) and both are demonstrated in the example projects.

## Strategy #1: External Verification

The advantage of this strategy is that it requires no special work inside your service. The drawback is that it does require you to be able to run the service locally and to be able to set up any state you might require using provider states or some other mechanism.

You will need two terminal windows, both of which are open in the `provider` directory.

In the first terminal window, run `sbt run` to start the service.

In the second terminal window, run:
```
sbt "pact-verify --source delivered_pacts/ --host localhost --port 8080"
```
*Note: the `--source delivered_pacts/` bit that tells the verifier you want to use local pact files in the directory specified.*

This will replay the request(s) in the Pact contract file against the service and check that the responses match.

## Strategy #2: Internal Verification

The advantage of this strategy is that setting up state is probably not an issue, you have full access to your project code base and running the verification is as simple as running your test cases (if you set it up correctly). The drawback is that you will need to do more work on your service to make sure your service can be run from inside a test case and that your core business logic can be mocked out.

To run the verification, all you have to do is navigate to the `provider_tests` directory and run `sbt test`. That's it.

### Additionally... Provider States
This example setup contains one extra thing, which is an example of how to use provider states. The test suite declares a `given("...")` requirement and it is fulfilled during external verification by the function in the `pact.sbt` at the root of the provider project.

The internal verification does not make use of provider states since the services setup happens during the `beforeAll()` and `afterAll()` functions. However, the mechanism for making use of provider states does exist, you just need to replace `.noSetupRequired` with something like:

```
.setupProviderState("given"){
  key =>
    key match {
      case "Document 1234 exists" =>
        // Some code to inject document 1234
        true
      case _ =>
        // Didn't find the key, but maybe there wasn't one for this contract.
        // So no point failing yet.
        true
    }
}
```

The function signature is `String => Boolean` where the `String` is the key value of the pact contract and `Boolean` is a way for you to say if your setup operation was a success or a failure.

## Things to note
1. The consumer and provider scala projects both use different case classes to represent the data. This does not matter, only the Pact file matters.
1. The consumer and provider scala projects both use different json libraries to read/write the json. This does not matter either, the contract is in no way less solid for the difference in library or marshalling logic.
