
# Strict mode
Scala-Pact was designed on a different set of choices than the original Pact implementations. The biggest but also least obvious of which is that Scala-Pact is subtly more forgiving by default in terms of how it matches.

For example, although the JSON specification states that arrays are ordered, you generally can't rely on that and your code should not be making that assumption. As such Scala-Pact considers the following a match:

`["red", "blue"] == ["blue", "red"]`

Whereas the original Pact implementations consider that a mismatch.

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

The original Pact matching process does not consider these a match. For anyone wishing to replicate that behaviour (and so we can call this library compliant!), a new strict mode has been introduced.

Any of the usual commands will now take a `--strict true` or `--strict false` parameter that defaults to false.

To use strict testing in the test frameworks you can use the `forgeStrictPact` initialiser in the consumer tests or do `.runStrictValidationTest(...)` in the verifier tests.

Please note that there is no such thing as a strict or non-strict Pact Contract file, the difference is entirely in the interpretation of the contract.
