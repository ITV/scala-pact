# ScalaPact
Library for generating Consumer Driven Contract files following the PACT standard using ScalaTest.

## Documentation TODO's
- Motivation
- How to use
- Examples
- Links to other pact resources

## Development TODO's
- Publish lib (only publish local supported currently)
- Improve error reporting
- Go back to immutability and add a key to pacts to generate multiple files, also then need to improve test script
- Normalise consumer & provider names before using them as the file name
- Improve builder so that case class public vars are not visible during build 

## Known issues
- Each tests runs all the accumulated tests
- Only supported header is content-type