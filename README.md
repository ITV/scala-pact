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

## Scala project library dependencies
The Pact integration test library itself depends on two Scala/Java libraries.

### Json4s
[Json4s](https://github.com/json4s/json4s) is used to create the JSON that is written to the Pact contract files.

### WireMock
[WireMock](http://wiremock.org/) is used to supply the mocks that ScalaPact runs the integration tests against.

## Documentation TODO's
- How to use
- Examples
- Links to other pact resources
- Running a stub service
- Pact proxy provider to verify

## Development TODO's
- Publish lib to repo
- Improve error reporting
