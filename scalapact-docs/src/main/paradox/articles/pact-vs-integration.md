# Pact vs Integration
Technically, when you write a Pact test you are creating an integration test, in that:

1. You write some client code to make the call to your provider;
2. You then write a test using a mock that expects a request and gives a response to a real HTTP call;
3. You check the results are what you expected.

The *purpose* of Pact and Integration tests is different though. A Pact test is there to describe the agreed contract between one service and another from the perspective of the consumer. An integration test can describe the relationship but not in a way that you can share with your provider for verification. Additionally Integration tests are good for testing failure cases where Pact tests are not.

Consider these two statements:
1. Pact tests define *what* the agreement between a consumer and a provider is
2. Integration tests check *how* that agreement is implemented on the consumer side

For instance, you should use Pact tests for describing the agreement:

1. Requesting data in a specific format from a provider
1. Describing content negotiation
1. How a provider would respond if it couldn't find the data you wanted

You could then build on that with integration tests:

1. Checking what happens if the provider simply isn't there
1. Network failures
1. Timeouts
1. Missing end points
1. Badly formed responses
