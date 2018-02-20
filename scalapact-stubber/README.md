# Overview
This project allows one or more stub servers to be launched with control over the location of the pact files, the port and the
SSL context that the project will use.   

# Config

An example config file is
```
 servers{  // marker 
       as-https {                    # the 'name' of the server. This has very little impact, but is useful for structuring the servers
           host: localhost           # Optional parameter, defaults to localhost
           port: 9000                # the port the server will run on
           directory: "target/pacts" # the directory the server will load pacts from. They must be .json files
           provider: "SomeName"      # An optional filter than means only the files for that provider will be loaded. It defaults to 'load all files' if this is missing
           ssl-context {             # If present the server will be an https server. The following parameters should be obvious 
               keymanager-factory-passphrase= "mypass"     
               keystore = "crypto/keystore.jks"
               keystore-password = "mypass"
               truststore = "crypto/truststore.jks"
               truststore-password = "changeit"
               client-auth = false
           }
           errorsAbort: false        #Does the server abort if a single pact file is flawed?
       }
       as-http {
           port: 9001
           directory: "target/pacts"
           errorsAbort: false
       }
    }
}
```

#Usage 1 - From tests

A common testing strategy is to first use pacts to test lowlevel code: for example the clients that take domain objects, send them to the remote api,
and turn the result into a domain object. After they are working acceptance tests can be written that hit the endpoints of the service
under test, and use the previously defined mocks.

To support this, the `ConfigBasedStubber` can be used: either from a config file, or by code. Example code for this is shown here:


```
    //Loads up the ssl context from the System properties. If you want you can construct it manually.
    val contextData = SSLContextData.fromSystemProperties()
    
    //loads all the pacts for the 'Name' provider
    val nameSpec = ServerSpec.forHttpsValidation("name", HttpsPorts.name, "target/pacts", Some("Name"), false,contextData, true)
    
    //loads all the pacts for the 'Address' provider
    val addressSpec = ServerSpec.forHttpsValidation("address", HttpsPorts.address, "target/pacts", Some("Address"), false,contextData, true)
    
    var stubber: ConfigBasedStubber
    var  executors: ExecutorService

    //code to be executed in your test framework of choice in beforeAll
    def setUpService() {
        val resources = ResourceBundle.getBundle("messages");
        executors = Executors.newFixedThreadPool(10);
        stubber = ConfigBasedStubber.apply(nameSpec, addressSpec, resources, executors);
    }

    //code to be executed in your test framework of choice in afterAll    
    public static void killStubber() {
        stubber.shutdown();
        executors.shutdownNow();
    }
```
 
The config based stubber can also be created (in a test) from a config file, using `ConfigBasedStubber(new File("path/to/configfile"))`

# Usage 2 - As a standalone utility

From sbt you can invoke the stubber command. The following arguments are useful
* --runTests true/false: this means that all the tests will (or won't) be run before the stubber launches (this should regenerate the pacts) Default is true
* --file "path/to/file": this says 'use the config file at the given location' 

