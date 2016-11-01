
# Pact Broker
During a build process you will need some mechanism for delivering the Pact files your consumer tests generate to your provider ready for verification.

You can do this any way you like, they are just JSON files after all, but we are currently exploring [pact-broker](https://github.com/bethesque/pact_broker). Pact Broker is a Ruby service that allows you to post and look up versioned Pact files.

*An opinionated note on versioning:* Focus on versioning your API's not your Pact files! It can be handy to have versions of Pact files around but you should not be pushing a breaking API changes and communicating them to your provider with versioned Pact files. A breaking API change is a new API version. Theoretically your provider should always be able to ask for the latest Pact files for each API version and have confidence that they are correct.
