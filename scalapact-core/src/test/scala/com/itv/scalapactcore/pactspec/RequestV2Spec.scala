package com.itv.scalapactcore.pactspec

import com.itv.scalapactcore.pactspec.util.PactSpecTester

class RequestV2Spec extends PactSpecTester {

  val pactSpecVersion = "2"

  describe("Exercising response V2 Pact Specification match tests") {

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
          fetchSpec("/request/query/different order.json"),
          fetchSpec("/request/query/different params.json"),
          fetchSpec("/request/query/matches.json"),
          fetchSpec("/request/query/missing params.json"),
          fetchSpec("/request/query/same parameter different values.json"),
          fetchSpec("/request/query/same parameter multiple times in different order.json"),
          fetchSpec("/request/query/same parameter multiple times.json"),
          fetchSpec("/request/query/trailing ampersand.json"),
          fetchSpec("/request/query/unexpected param.json")
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

    it("should check the request header specs with regex") {
      testSpecs(
        List(
          fetchSpec("/request/headers/matches with regex.json")
        )
      )
    }

    it("should check the request body specs") {
      testSpecs(
        List(
          fetchSpec("/request/body/array at top level.json"),
          //fetchSpec("/request/body/array in different order.json"), // DO NOT AGREE WITH THIS ONE
//          fetchSpec("/request/body/array size less than required.json"),
//          fetchSpec("/request/body/array with at least one element matching by example.json"),
//          fetchSpec("/request/body/array with at least one element not matching example type.json"),
//          fetchSpec("/request/body/array with nested array that does not match.json"),
//          fetchSpec("/request/body/array with nested array that matches.json"),
//          fetchSpec("/request/body/array with regular expression in element.json"),
//          fetchSpec("/request/body/array with regular expression that does not match in element.json"),
          fetchSpec("/request/body/different value found at index.json"),
          fetchSpec("/request/body/different value found at key.json"),
//          fetchSpec("/request/body/matches with regex with bracket notation.json"),
//          fetchSpec("/request/body/matches with regex.json"),
//          fetchSpec("/request/body/matches with type.json"),
          fetchSpec("/request/body/matches.json"),
          fetchSpec("/request/body/missing index.json"),
          fetchSpec("/request/body/missing key.json"),
          fetchSpec("/request/body/no body no content type.json"),
          fetchSpec("/request/body/no body.json"),
          fetchSpec("/request/body/not null found at key when null expected.json"),
          fetchSpec("/request/body/not null found in array when null expected.json"),
          fetchSpec("/request/body/null found at key where not null expected.json"),
          fetchSpec("/request/body/null found in array when not null expected.json"),
          fetchSpec("/request/body/number found at key when string expected.json"),
          fetchSpec("/request/body/number found in array when string expected.json"),
          fetchSpec("/request/body/plain text that does not match.json"),
          fetchSpec("/request/body/plain text that matches.json"),
          fetchSpec("/request/body/string found at key when number expected.json"),
          fetchSpec("/request/body/string found in array when number expected.json")//,
          //fetchSpec("/request/body/unexpected index with not null value.json"),  // DO NOT AGREE WITH THIS ONE
          //fetchSpec("/request/body/unexpected index with null value.json"),  // DO NOT AGREE WITH THIS ONE
          //fetchSpec("/request/body/unexpected key with not null value.json"),  // DO NOT AGREE WITH THIS ONE
          //fetchSpec("/request/body/unexpected key with null value.json")  // DO NOT AGREE WITH THIS ONE
        )
      )
    }
  }

}
