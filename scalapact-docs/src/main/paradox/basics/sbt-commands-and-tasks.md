# SBT Commands

## pact-test
You can run the Pact test cases just by executing `sbt test` as normal. Because of the way the library has been written, running the tests will generate a series of Pact JSON files, one for each interaction.

The generated Pact files will be output to your target directory under `<project root>/target/pacts`.

Usually it is desirable to condense the Pact files into one file per consumer / provider pair that contains all of the possible interactions.

Entering `sbt pact-test` will:

1. Run clean to remove any lingering pact files
1. Run the tests as normal to generate the Pact files
1. Squash the Pact files into one per consumer / provider pair

## pact-stubber
In order to test your service in isolation, you'll need to be able to stub out your service's upstream dependancies. Luckily, you've already defined all the behaviours you expect your provider to exhibit in your CDC Pact tests!

Running `sbt pact-stubber` will re-run your Pact tests and then use the generated Pact files to create a running stub service. The stub will accept requests and deliver the responses you defined in your test cases giving you predictable, maintainable behaviour.

You can also run the stubber using a combination of the following command line options, below are the defaults:

`sbt "pact-stubber --host localhost --port 1234 --source target/pacts"`

*Note that files in the source folder are recursively loaded.*

#### HTTP Administration
If you prefer, you can use the stubber dynamically by adding and removing pacts using HTTP. All calls must be made with a special header:

`X-Pact-Admin=true`

- `GET /interactions` returns a list of all currently loaded interactions
- `POST | PUT /interactions` accepts a Pact JSON string and adds all the interactions to the pool it matches against
- `DELETE /interactions` Clears all the current interactions so you can start again

## pact-publish
If you plan to use pact testing as part of your CI pipeline you'll probably want to be able to share pact files efficiently between builds. For example a consumer project's build generates a new version of the projects Pact files and they are then used during the providers CI build.

To achieve this we use the Ruby tool called Pact Broker (see below) and the publish command to update the files to it.

#### Command Line Options
Before we can publish, we have to tell Scala-Pact where it can find a running instance of Pact Broker by adding the following line to either `build.sbt` or `pact.sbt`:

`pactBrokerAddress := "http://my-pact-broker:4321"`

You can also specify the version you wish to publish under by adding:

`pactContractVersion := "1.0.0"`

If you omit this variable or set it to an empty string, the main project version will be the version used to publish against.

You can then use the publish command to generate and upload your pact files to pact broker:

`sbt pact-publish`

Note that your Pact files will have the same version number as the normal project version defined in your `build.sbt` file, because you versioned that breaking API change - right?

By default, Scala-Pact does not allow you to publish pact files from SNAPSHOT versions of your project (but takes into account the pactContractVersion if set). This is because it can confuse pact brokers understanding of the latest contract. If you wish to enable this behaviour, add the following line to your `pact.sbt` file:

`allowSnapshotPublish := true`

## pact-verify
Once the consumer has defined the contract as CDC tests and exported them to Pact files, they'll deliver them to their provider. The provider then exercises their own API using the Pact files via a verifier.

The verifier is quite a simple idea: load a Pact file, make all the requests and compare all the responses to the expected ones.

The Scala-Pact verifier can be run by entering `sbt pact-verify`.

The verifier will write out JUnit results to the `target/test-reports` directory in order to fail builds.

You can also invoke the verifier via a @ref:[test case](../articles/verification-strategies.md) if you prefer.

#### Command Line Options
You can also run the verifier using a combination of the following command line options. Below are the defaults:

`sbt "pact-verify --host localhost --protocol http --port 1234 --source pacts"`

*Note that files in the source folder are recursively loaded. Specifying a local source folder takes precedence over loading remote files from Pact Broker (see below)*

#### Verifying with Pact Broker during a CI build
If you're using the publish command to send files to Pact Broker, you'll also want to know how to verify against them in the provider project.

To do this, you need to add the following to either your `build.sbt` or `pact.sbt` file:

```
pactBrokerAddress := "http://my-pact-broker:4321"
providerName := "The Name Of This Service"
consumerNames := Seq("Consumer A", "Consumer B")
```

Note: The names are **keys** and all have to line up. Downstream services must publish with the same names that you use to retrieve against.

You then run verify as normal **without** specifying a local folder i.e.:

`sbt "pact-verify --host localhost --port 1234"`

This causes the verifier to try and load it's Pacts from Pact Broker ahead of the normal verification process.

## Other considerations

1. Mock servers can only understand an endpoint being in one state. Mostly that isn't a problem - if you want to create a Pact describing the look up of documents that result in a 200 or a 404 you simply look up two different documents. Where you have something like a `/status` endpoint that could come back in different states that you care about, you would have to be a bit creative, or not describe that behaviour in a pact contract.
1. ScalaTest runs in parallel by default so even clearing the state of the stubber between tests could, and probably would, result in errors if you were using HTTP administration.
