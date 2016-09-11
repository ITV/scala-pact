package com.itv.scalapactcore.pactspec

import com.itv.scalapactcore.pactspec.util.PactSpecTester

class RequestV1Spec extends PactSpecTester {

  val pactSpecVersion = "1"

  describe("Exercising response V" + pactSpecVersion + " Pact Specification match tests") {

    it("should check the request method specs") {
      testSpecs(
        List(
          fetchSpec("/request/method/different method.json"),
          fetchSpec("/request/method/matches.json"),
          fetchSpec("/request/method/method is different case.json")
        )
      )
    }

    it("should check the request path specs") {
      testSpecs(
        List(
          fetchSpec("/request/path/empty path found when forward slash expected.json"),
          fetchSpec("/request/path/forward slash found when empty path expected.json"),
          fetchSpec("/request/path/incorrect path.json"),
          fetchSpec("/request/path/matches.json"),
          fetchSpec("/request/path/missing trailing slash in path.json"),
          fetchSpec("/request/path/unexpected trailing slash in path.json")
        )
      )
    }

    it("should check the request query specs") {
      testSpecs(
        List(
          fetchSpec("/request/query/different param order.json"),
          fetchSpec("/request/query/different param values.json"),
          fetchSpec("/request/query/matches with equals in the query value.json"),
          fetchSpec("/request/query/matches.json"),
          fetchSpec("/request/query/trailing amperand.json")
        )
      )
    }

    it("should check the request header specs") {
      testSpecs(
        List(
          fetchSpec("/request/headers/empty headers.json"),
          fetchSpec("/request/headers/header name is different case.json"),
          fetchSpec("/request/headers/header value is different case.json"),
          fetchSpec("/request/headers/matches.json"),
          fetchSpec("/request/headers/order of comma separated header values different.json"),
          fetchSpec("/request/headers/unexpected header found.json"),
          fetchSpec("/request/headers/whitespace after comma different.json")
        )
      )
    }

    it("should check the request body specs") {
      testSpecs(
        List(
          fetchSpec("/request/body/array in different order.json"),
          fetchSpec("/request/body/different value found at index.json"),
          fetchSpec("/request/body/different value found at key.json"),
          fetchSpec("/request/body/matches.json"),
          fetchSpec("/request/body/missing index.json"),
          fetchSpec("/request/body/missing key.json"),
          fetchSpec("/request/body/not null found at key when null expected.json"),
          fetchSpec("/request/body/not null found in array when null expected.json"),
          fetchSpec("/request/body/null found at key where not null expected.json"),
          fetchSpec("/request/body/null found in array when not null expected.json"),
          fetchSpec("/request/body/number found at key when string expected.json"),
          fetchSpec("/request/body/number found in array when string expected.json"),
          fetchSpec("/request/body/plain text that does not match.json"),
          fetchSpec("/request/body/plain text that matches.json"),
          fetchSpec("/request/body/string found at key when number expected.json"),
          fetchSpec("/request/body/string found in array when number expected.json"),
          fetchSpec("/request/body/unexpected index with not null value.json"),
          fetchSpec("/request/body/unexpected index with null value.json"),
          fetchSpec("/request/body/unexpected key with not null value.json"),
          fetchSpec("/request/body/unexpected key with null value.json")
        )
      )
    }
  }

}
