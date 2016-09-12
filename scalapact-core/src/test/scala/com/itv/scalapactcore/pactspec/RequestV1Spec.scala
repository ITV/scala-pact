package com.itv.scalapactcore.pactspec

import com.itv.scalapactcore.pactspec.util.{NonStrictOnly, PactSpecTester}

class RequestV1Spec extends PactSpecTester {

  val pactSpecVersion = "1"

  describe("Exercising response V" + pactSpecVersion + " Pact Specification match tests") {

    it("should check the request method specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/method/different method.json")(NonStrictOnly),
          fetchRequestSpec("/request/method/matches.json")(NonStrictOnly),
          fetchRequestSpec("/request/method/method is different case.json")(NonStrictOnly)
        )
      )
    }

    it("should check the request path specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/path/empty path found when forward slash expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/path/forward slash found when empty path expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/path/incorrect path.json")(NonStrictOnly),
          fetchRequestSpec("/request/path/matches.json")(NonStrictOnly),
          fetchRequestSpec("/request/path/missing trailing slash in path.json")(NonStrictOnly),
          fetchRequestSpec("/request/path/unexpected trailing slash in path.json")(NonStrictOnly)
        )
      )
    }

    it("should check the request query specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/query/different param order.json")(NonStrictOnly),
          fetchRequestSpec("/request/query/different param values.json")(NonStrictOnly),
          fetchRequestSpec("/request/query/matches with equals in the query value.json")(NonStrictOnly),
          fetchRequestSpec("/request/query/matches.json")(NonStrictOnly),
          fetchRequestSpec("/request/query/trailing amperand.json")(NonStrictOnly)
        )
      )
    }

    it("should check the request header specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/headers/empty headers.json")(NonStrictOnly),
          fetchRequestSpec("/request/headers/header name is different case.json")(NonStrictOnly),
          fetchRequestSpec("/request/headers/header value is different case.json")(NonStrictOnly),
          fetchRequestSpec("/request/headers/matches.json")(NonStrictOnly),
          fetchRequestSpec("/request/headers/order of comma separated header values different.json")(NonStrictOnly),
          fetchRequestSpec("/request/headers/unexpected header found.json")(NonStrictOnly),
          fetchRequestSpec("/request/headers/whitespace after comma different.json")(NonStrictOnly)
        )
      )
    }

    it("should check the request body specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/body/array in different order.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/different value found at index.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/different value found at key.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/matches.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/missing index.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/missing key.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/not null found at key when null expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/not null found in array when null expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/null found at key where not null expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/null found in array when not null expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/number found at key when string expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/number found in array when string expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/plain text that does not match.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/plain text that matches.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/string found at key when number expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/string found in array when number expected.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/unexpected index with not null value.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/unexpected index with null value.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/unexpected key with not null value.json")(NonStrictOnly),
          fetchRequestSpec("/request/body/unexpected key with null value.json")(NonStrictOnly)
        )
      )
    }
  }

}
