# Roadmap

This is a brief outline of the intended sequence of upcoming work that will result in a 3.0.0 release.

##Phase 1 - Better errors
Scala-Pact's major pain point at the moment, is efficiently tracking down exactly where a match failed. It's laborious and manual. During early development this was seen as low priority since we were often testing small payloads and initially there was no support for matching rules - spotting issues was pretty trivial.

It feels important to fix this before any other big lumps of work are undertaken, such as adding support for AMQP.

Order of work:

1. Rework the matching logic (in progress)
1. Add in the ability to aggregate failures
1. Find a nice way to present them

##Phase 2 - Ironing out the niggles
Two parts to this really:

1. There's a bunch of outstanding issues raised against the project that need some form of attention.
1. The project needs some hygiene maintenance which mostly means moving to a multi-project build and considering whether we need three Scala-Pact SBT library dependencies or whether we could get away with less. Certainly two seems possible (plugin and test framework).

##Phase 3 - Fix the dependencies
The aim here is to push out as many dependencies on other libraries as possible, so that we don't interfere with existing projects people want to use Scala-Pact with. The big one is Http4s, because it drags in versions of Scalaz and Argonaut. Pushing these out (into modules perhaps?) might mean we ultimately need more SBT dependencies again, but hopefully better planned ones!

##Phase 4 - AMQP? Pact version 3 or 4?
I think I could characterise the previous three phases as "Stop it and tidy up". That may seem a little dull but I'd rather we were doing a good there first before we move on.

Up next is almost certainly related to adding support for AMQP and seeing where that takes us in relation to keeping up with the Pact standards.
