The changes to Scala Pact of SSL Support are as follows

## The SslContextmap
Implicitly in scope should be an SslContextmap. This is a map from strings to SSL Contexts. Example code to create one is

```
      val thisSslContext = SslContextMap.makeSslContext("./src/test/resources/localhost.jks", "somePassWord", "./src/main/resources/truststore.jks", "somePassWord")
      implicit val sslContextMap: SslContextMap = new SslContextMap(Map("default" -> thisSslContext))
```

## Specifying the SSL Context for the scala pact mock server
When creating a fact using 'forgePact' there is a new method on the forger called addSslContextForServer.
These is every chance that this will be a different SSL context to the one used by the clients making calls

```
    forgePact
        .between("risk assessment service")
        .and("risk assessment client")
        .addSslContextForServer("default")
        .addInteraction(
```

## Specifying the SSL Context when forging a pact for the Http Clients
The interactions allow an SslContextName to be added for each interaction. This has no effect at all when creating the pacts, but is added to the pact json and 
is used when validating pacts (see below). Ideally the certificates used will match the certificates used by the code
under test

```
        .addInteraction(
          interaction
            .description("Risk Assessment API says NotOk")
            .given("Risk Assessment API suspects fraud")
              .withSsl("someSslName")
```

This means that when the pact verifier is running, it can change which ssl certificate it uses. 

## Storing the SSL context name in the pact json
The scala pact mock server   `.addSslContextName` doesn't need to be stored. The `withSsl("someSslName")` does. Because the pact json is to be
shared across many languages and software projects, it's not a good idea to change it. Thus the `someSslName` is coded up as a fake header. This fake header will not be actually sent by the verifier. The fake header has the name `pact-ssl-context`

## Validation with SSL context name
This is requires the use of a unit test at the moment. For example:

```
     val thisSslContext = makeSslContext("./src/test/resources/localhost.jks", "somePassWord", "./src/main/resources/truststore.jks", "somePassWord")
     implicit val sslContextMap: SslContextMap = new SslContextMap(Map("default" -> thisSslContext))

     verifyPact
            .withPactSource( .. some source ... )
            .noSetupRequired 
            .runVerificationAgainst("https", "localhost", sslPort, 40 seconds)
```
This code will load the JSON file. If the interaction has a  `pact-ssl-context` it will use that 
header to select an SSL context from the SslContextMap that is in scope.


# SSL and stubbing
 See the readme in the project scala-pact stubber

# KNOWN ISSUES

## Pact Verifier doesn't currently verify SSL
To do this I'd have to know how to setup SSL Context Maps in SBT and pull them into commands

## Assumption that all the pacts are either http or https
The run verification against method above assumes that all the pacts are http or https. It would probably 
be better if the following worked. It currently doesn't

```
     verifyPact
            .withPactSource( .. some source ... )
            .noSetupRequired 
            .runVerificationAgainst("https", "localhost", sslPort, 40 seconds)
            .runVerificationAgainst("http", "localhost", httpPort, 40 seconds)
```

## The Pact JSON uses a customer-header to describe which SSL Context is being used
This is probably better than using a custom modification to the JSON. Perhaps it should be made configurable...

