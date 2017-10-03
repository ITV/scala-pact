
# Strict mode
Scala-Pact was designed on a different set of choices than the original Pact implementations. The biggest but also least obvious of which is that Scala-Pact is subtly more forgiving by default in terms of how it matches.

#### Formally:
Scala-Pact has two matching modes called strict and permissive. Strict mode follows the official Pact specification to the letter, and permissive follows a select sub-set of the specification that makes writing contract tests easier. Permissive is the default in Scala-Pact.

**Important Note:** When testing between Scala services that both use Scala-Pact, the default permissive mode is more convenient. However, ***when dealing with another Pact implementation you should ALWAYS use strict mode*** since that is what they are based on.

#### For example...
Although the JSON specification states that arrays are ordered, you generally can't rely on that and your code should not be making that assumption. As such Scala-Pact considers the following a match:

`["red", "blue"] == ["blue", "red"]`

Whereas the original Pact implementations consider that a mismatch and would need rules added for them to be acceptable.

To take body matching as an example, Scala-Pact's matching process states that as long as the expected document exists within the actual document, they are a match. This is very powerful because it allows you to only describe the parts of the response object you care about (**without using body matching rules!**). For example these two nonsense JSON blobs are a match:

Expected (the part of the contract we care about) data and structure:
```
{
  "id": "abc123",
  "metadata": {
    "name": "Bob"
  },
  [
    {
      "favouriteColour": "red"
    }
  ]
}
```

Actual:
```
{
  "metadata": {
    "likesSports": true,
    "name": "Bob",
    "employeeNumber": 15666884
  },
  "id": "abc123",
  [
    {
      "favouriteColour": "green",
      "takesSugar": true
    },
    {
      "favouriteColour": "red",
      "likesFishing": null
    }
  ]
}
```

Notice that despite the extra data, the expected does fit perfectly inside the actual.

On the other hand the original Pact matching process does not consider these a match. For anyone wishing to replicate that behaviour (and so we can call this library compliant!), a new strict mode has been introduced.

Any of the usual commands will now take a `--strict true` or `--strict false` parameter that defaults to false.

To use strict testing in the test frameworks you can use the `forgeStrictPact` initialiser in the consumer tests or do `.runStrictValidationTest(...)` in the verifier tests.

**Please note** that there is no such thing as a strict or non-strict Pact Contract file, the difference is entirely in the interpretation of the contract.

## So why would anyone use strict mode?
Two good reasons:
1. As previously mentioned, Scala-Pact is completely compatible with other Pact implementations but they only support what we call strict matching. When dealing with them, you should too so there are no misunderstandings!
2. You get better error messages on match failure. Since strict matching is less forgiving, Scala-Pact can do a better job of telling you where contracts were misaligned.

## Wait! Then why would anyone use permissive matching?!
Provided both the services are using Scala-Pact for their CDC testing, permissive matching is much easier to use and understand, you can almost get away with no matching rules.
