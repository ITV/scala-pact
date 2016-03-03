# ScalaPact
Library for generating Consumer Driven Contract files in Scala projects following the PACT standard using [ScalaTest](http://www.scalatest.org/).

## Motivation
[Pact](https://github.com/realestate-com-au/pact) is an implementation of CDC testing ([Consumer Driven Contract testing](http://martinfowler.com/articles/consumerDrivenContracts.html)). There are other implementations like [Pacto](https://github.com/thoughtworks/pacto) and they vary slightly in how they interpret the testing process.

Following the Pact implementation of CDC, the process goes something like this:

1. Write a piece of client code in a service consumer project that knows the details of how to connect to a provider.
2. Write a real integration test for that client code that hits a mock, but also as a side effect emits a JSON file describing the relationship. This JSON file is your Pact or Contract file. Note that in Pact (not Pacto) it is owned by the consumer.
3. During development of the consumer, the Pact file can be used to run a lightweight stub service that mimics the expected behavior of the provider.
4. Give the generated Pact file to the team who build the provider, an upstream service you depend on. This Pact file tells the provider team both how you expect their API to behave and which parts of their API are delivering value to you the consumer.
5. The provider then verifies the Pact by running the requests and responses described in the Pact file against their system.

Most of the tools for CDC testing have come out of the Ruby community with a few other implementations for other languages here and there. The crucial point to appreciate is that the client code is written in your native language, in your native languages test framework and therefore your Pact integrations tests should be too. The other uses of the Pact files, verification and stubbing are largely standalone processes that are language agnostic and so it's quite feasible to use the Ruby implementations.

There is an implementation of the Pact integration test suite that is theoretically compatible with Scala and [Specs2](https://etorreborre.github.io/specs2/) called [Pact-JVM](https://github.com/DiUS/pact-jvm). Pact-JVM also theoretically supports other JVM languages like Java and Groovy.

In practice we had found there were issues with Pact-JVM integration test solution for Scala (via Specs2). There was a choice between investing time to help fix Pact-JVM or writing a replacement library. As Pact-JVM is a general purpose library across the JVM, working on it was an unknown quantity and it was deemed worth prototyping a replacement. ScalaPact is the result.

## ScalaPact setup
ScalaPact is not a published library yet. For the time being, we recommend that you checkout the project and do a local SBT publish in order to use it as a dependency. Alternatively you can compile the JAR and place it manually in the unmanaged lib folder of your project.

If you decide to do a local publish, you will then need to add the dependency to your build.sbt file like this:

```
libraryDependencies ++= Seq(
  "com.itv" %% "scalapact" % "0.0.1-SNAPSHOT" % "test"
)
```

## Other tools you will need to install

### Ruby
You will need Ruby. I can only apologise.

### Pack Mock Service
ScalaPact allows you to generate JSON Pact contract files.

Once you have a contract file you will probably want to be able to run a stub version of your provider to test against. [Pact Mock Service](https://github.com/bethesque/pact-mock_service/) is a Ruby tool that allows you to run an HTTP service that provides a mock based on your Pact file, and is administered via HTTP requests. Installation instructions can be found on the projects [github page](https://github.com/bethesque/pact-mock_service/).

### Pact Provider Proxy
If you're a provider and you need to verify a Pact contract against your service you'll need a verifier. There is a Ruby Pact verifier but it is bound to Ruby projects and expects to be run from within a Ruby test suite. Fortunately there is a gem call Pact Provider Proxy that... proxies provider Pact requests to the verifier.

There is an instance of Pact Provider Proxy in this project in the Pact verifier subfolder that you can install using Ruby's `bundle install`.

## Basic usage examples
There is an example test spec that can be found [here](https://github.com/ITV/ScalaPact/blob/master/src/test/scala/com/itv/scalapact/ExampleSpec.scala). The hope is that this will be a living example spec.

## Considerations
1. Mock servers can only understand one endpoint being in one state. Mostly that isn't a problem, if you want to create a pact describing the look up of documents that result in a 200 or a 404 you simply look up two different documents. Where you have something like a `/status` endpoint that could come back in different states that you care about, you would have to be a bit creative, or not describe that behaviour in a pact contract!
- ScalaTest runs in parallel so even clearing the state between tests could, and probably would, result in errors.

## Pact tests VS Integration tests
Technically, we you write a Pact test you are creating an integration test in that:
1. You write some client code to make the call to your provider;
2. You then write a test using a mock that expects a request and gives a response to a real http call;
3. You check the results are what you expected.

The *purpose* of Pact and Integration tests is different though. A Pact test is there to describe the agreed contract between one service and another from the perspective of the consumer. An integration test can describe the relationship but not in a way that you can share with your provider for verification. Additionally Integration tests are good for testing failure cases where Pact tests are not.

Consider these two statements:
1. Pact tests define *what* the agreement between a consumer and a provider is
2. Integration tests check *how* that agreement is implemented on the consumer side

For instance:

You should use Pact tests for describing the agreement:
1. Requesting data in a specific format from a provider;
1. Describing content negotiation;
1. How a provider would respond if it couldn't find the data you wanted.

You could then build on that with integration tests for...
1. Checking what happens if the provider simply isn't there;
1. Network failures;
1. Timeouts;
1. Missing end points;
1. Badly formed responses.

## Pact specification compliance level
Currently ScalaPact is not 100% comliant with the official Pact specification. We plan to be but the library is still under active development. The roadmap to Pact compliance will be something like:
1. Test all tools against the official specification test cases;
1. Implement provider states;
1. Consolidate our process with the official implementors guide;
1. Implement the Json body special case.

At the moment we do some level of validation by running our pact contracts against the other Pact tools on the market in an attempt to catch any glaring problems.

### Why aren't we Pact compliant?
There is already more than one CDC implementation but this one is closest to Pact, and at this time relies on some of their tooling.

That said, Pact was created for a company to meet it's needs and ScalaPact has been created in the same vein. We have a problem that the official Pact tools don't quite solve, and we're building ScalaPact to meet our requirements.

Currently, it is our intention to conform to Pact as closely as possible rather than splinter into another implementation.

## Acknowledgments
ScalaPact is not an original idea, we're standing on the shoulders and borrowing heavily from the work of the following groups and people:
1. [DiUS](https://github.com/DiUS)
1. [Pact Foundation](https://github.com/pact-foundation)
1. [Thoughtworks / Ian Robinson / Martin Fowler](http://martinfowler.com/articles/consumerDrivenContracts.html)

## Scala project library dependencies
The Pact integration test library itself depends on two Scala/Java libraries.

### Json4s
[Json4s](https://github.com/json4s/json4s) is used to create the JSON that is written to the Pact contract files.

### WireMock
[WireMock](http://wiremock.org/) is used to supply the mocks that ScalaPact runs the integration tests against.

## Documentation TODO's
- How to use
- Examples
- Add note about "gem install bundler"
- Links to other pact resources
- Add a section on Pact Broker
- Describe running a stub service with pack-mock-service
- Describe using Pact proxy provider to verify
- Update all README.md files
  - Legal hygiene checks, make sure there's nothing offensive
  - Public consumption, is it easy to understand what you have to do to use ScalaPact?
  - Fullest description should be in the parent README, sub projects should reference the parent but otherwise provide lean quickstart information and examples. This is the area the existing pact tools are worst at so lets get it right!

## Short term development TODO's
- Rename repo: ScalaPact -> scalapact
- Publish lib to repo
- Improve error reporting?
- Create tmp pact files with sha1 suffix not description text (now that we have the pact plugin there's no need for them to be human readable and risk duplicates) THEN see if the description is used anywhere else and remove if not. e.g.
  - from: my-consumer_my-provider_in-some-context
  - to: my-consumer_my-provider_akshbdk3as43hdb1ak

## Mid term development TODO's
- Implement the JSON body special case
- Write tests to run matcher code against official pact spec tests

## Longer term development TODO's
