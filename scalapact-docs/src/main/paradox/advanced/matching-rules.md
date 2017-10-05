
# Matching rules
Pact testing at it's heart, is about testing the behaviour of systems that are in a ***known state***.

The known state in the consumer tests is provided by you while you write the test.

The known state during verification is achieved through @ref:[provider states](provider-states.md) (external verification) or using beforeAndAfter clauses in your test suite (internal verification). Read up on @ref:[verification strategies](../articles/verification-strategies.md) for more details.

Sometimes however, achieving a good known state during verification is difficult. For example, if a field in a JSON body is based on the current time, it may be hard to set up your service with a fixed time in order to verify by a straight comparison approach.

To help matters, we have matching rules, which in the pact contracts, look like this:

```
"$.body.alligator.name": {"match": "regex", "regex": "\\w+"}
```

What we have there is a path to a field in a body, and a requirement that if a normal comparison fails, as long as the fields value fits this regex, then it can be considered a pass.

## Rule application
Rules can be applied to both requests and responses.

The way to think of them is:

- **Request** matching rules are **stubbing** rules
- **Response** matching rules are **verification** rules

## Rule syntax examples
Example response matching rules:
```
.willRespondWith(
  ...
  matchingRules =
    headerRegexRule("fieldA", "\\w+")
    ~> bodyTypeRule("path["to"].fieldB")
    ~> bodyRegexRule("path.to[*].fieldC", "\\w+")
    ~> bodyArrayMinimumLengthRule("path.to.fieldD", 2)
)
```
**Note:** Unlike the JSON representation above, the rule paths do not start with `$.body.`.

## PactPath (spoiler, it's JsonPath with extra bits.)
To the experienced eye, the path syntax may look like JsonPath... and that's because it is JsonPath. Mostly.

In fact it's a subset of JsonPath with a few extra bits bolted on top to allow for XML bodies. The bolt ons are:

- Text elements e.g. `fish.chips[*].ketchup['#text']`
- Attributes e.g. `fish.chips[*].ketchup['@applied']`

## Types of Matching Rule

#### Regex Rules
Probably the most obvious rule is the regex rule. Does the value match this regex?

Note that attempts are made to satisfy this requirement in non-obvious ways. For example if the regex is a "\\d" then we will convert a numeric field to a string to test it.

#### Type Rules (considered dangerous)
I think of this really as a structural check. Does the structure under this path match the structure of the provided example (numbers are numbers, strings are strings, object are objects etc.) regardless of the values.

Best practice: Use type rules as close to the leaf nodes of documents as possible.

#### Minimum array length
As you'd expect, the received array must be of at least this length.

## Matching Rule Best Practices
Matching rules excel in areas that are prone to vary by environment, are non-deterministic in someway, or that vary over time.

Excellent uses for matching rules include:

- Time dependent fields and date matching
- Checking generated tokens
- Url's where the host varies but the end point remains constant.
- Search query results where the number of results doesn't matter, as long at the structure is correct.

## Permissive vs Strict
The matching rules apply in both modes, but really matching rules were designed to be used with strict mode. Generally speaking you can get away with far less rules if you can use permissive matching in the first place.

For example: Minimum array length is not an issue in permissive matching as long as the array items in the expected document exist within the received document.

In effect, matching rule largely make up for the lack of flexibility that strict mode provides (with the caveat that strict mode does give better error reports if you're prepared to put the work in).

## Words of caution...
If you can avoid rules, do.

Rules overlap other rules, and rules are all tested in no particular order, and the more rules you have the more chances you have of accidentally matching when you shouldn't (or vice versa).

The most dangerous rule is the type rule. Consider this made up document:

```
{
  "person": {
    "names": {
      "first": "hello"
    }
  }
}
```

If you had a regex that required `person.names.first` to equal "Sally" or "Fred" (i.e. `bodyRegexRule("person.names.first", "Sally|Fred")`) it would fail on "hello".

However, if you also had a type rule on names like `bodyTypeRule("person.names")` or `bodyTypeRule("person.names.*")` ...should the match pass or fail?

As it is, it would fail and that may seem obvious, but hopefully you can see how test cases can become difficult to reason about the more rules you add.
