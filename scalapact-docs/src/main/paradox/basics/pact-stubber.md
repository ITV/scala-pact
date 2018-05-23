# Pact Stubber

Scala-Pact comes bundled with the ability to use Pact contracts to run stub versions of services.


## Stubbing with the plugin

The most convenient use of the stubber is to run it from within you consumer project. This gives you a running service that pretends to be all of your upstream dependencies at once to help with local testing. This is achieve with the `pactStub` or more thorough `pactStubber` tasks. See @ref:[SBT Commands](sbt-commands.md).

## Standalone stubber

If you want to run a Pact based stub service without SBT or for Pact's you aren't generating, we also have a standalone implementation. The usage of which is identical to the sbt command.

Just one catch, you need to build your own jar! Roughly like this assuming you have java and SBT installed:

```
git clone git@github.com:ITV/scala-pact.git
cd scala-pact
sbt standalone/assembly
cp scalapact-standalone-stubber/target/scala-2.12/pactstubber.jar .
```

..and then you can run it like this where "pacts" is a folder containing pact json contracts:

```
java -jar pactstubber.jar --port 8080 --source pacts
```

..and then call it, in this case using `curl` to a `/results` endpoint defined in one of the loaded contract files:

```
curl localhost:8080/results
```
