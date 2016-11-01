# Example projects

@@@ index

* [The Consumer](consumer.md)
* [The Provider](provider.md)

@@@

## Overview
The three projects presented here represent a simple CDC scenario.

There is one consumer project that owns and generates the Pact contract.

We then have two providers projects. Having multiple providers is very normal but in this case, the two providers are **the same provider** (do the same job) but the verification is done in two completely different ways for demonstration purposes.

Each project contains the bare minimum to demonstrate how pact testing works and are not supposed to represent best coding practices in general.

The examples don't go into great depth, for more information you should refer to the @ref:[basic](../basics/index.md) and @ref:[advanced](../advanced/index.md) guides.

### Which verification method should I use?
Please refer to the @ref:[verification strategies guide](../articles/verification-strategies.md).

## Setup assumptions
These example projects are designed to be run on linux or mac in that they make use of a simple shell script. It is also assumed that you have Scala and SBT setup.
