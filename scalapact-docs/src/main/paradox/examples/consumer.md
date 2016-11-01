# The Consumer
The consumer project owns and generates the Pact contract file.

The Pact contract file is the result of an [integration test](https://github.com/ITV/scala-pact/blob/example-projects/example/consumer/src/test/scala/com/example/consumer/ProviderClientSpec.scala) that tests a [real, albeit crude piece of client code](https://github.com/ITV/scala-pact/blob/example-projects/example/consumer/src/main/scala/com/example/consumer/ProviderClient.scala) against a mock.

To run the pact-tests, do the following (assuming you've checked out the project and are in the example directory):

```
cd consumer
sbt pact-test
```

You should now be able to see a file in the `consumer/target/pacts/` directory called `Consumer_Provider.json`. This is your Pact file that describes the behaviour you expect fro the Provider service.

##Stubbing
If you wanted to, you could now run a stub version of the Provider service, again from the `consumer` directory, by simply entering `sbt pact-stubber` on the command line and the service would start on port `1234`.

Try it, you should be able to go hit [the results end point in your browser](http://localhost:1234/results).

You could also use the [standalone stubber](https://github.com/ITV/scala-pact/tree/master/scalapact-standalone-stubber). If you plan to use this in a CI pipeline you should consider assembling it into a JAR by running `sbt assembly`.

## Delivering the pact file
In a real scenario, you would probably use Pact Broker to send the newly created Pact contract to the Provider ahead of verification.

In our demo, we're going to skip that on the assumption that you don't have a [Pact Broker](https://github.com/bethesque/pact_broker) handy, but full details of how to integrate with [Pact Broker](https://github.com/bethesque/pact_broker) can be found in the main [README](https://github.com/ITV/scala-pact/blob/master/README.md) file.

Instead we're going to use a script to do the delivery. Navigate back to the main example directory on the terminal and run:
```
bash deliver.sh
```

This script simply copies the `Consumer_Provider.json` file into the `provider/delivered_pacts` directory.
