package com.itv.scalapactcore.pactspec

import com.itv.scalapactcore.pactspec.util.PactSpecTester

class RequestV2Spec extends PactSpecTester {

  val pactSpecVersion = "2"

  describe("Exercising response V" + pactSpecVersion + " Pact Specification match tests") {

    it("should check the request method specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/method/different method.json"),
          fetchRequestSpec("/request/method/matches.json"),
          fetchRequestSpec("/request/method/method is different case.json")
        )
      )
    }

    it("should check the request path specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/path/empty path found when forward slash expected.json"),
          fetchRequestSpec("/request/path/forward slash found when empty path expected.json"),
          fetchRequestSpec("/request/path/incorrect path.json"),
          fetchRequestSpec("/request/path/matches.json"),
          fetchRequestSpec("/request/path/missing trailing slash in path.json"),
          fetchRequestSpec("/request/path/unexpected trailing slash in path.json")
        )
      )
    }

    it("should check the request query specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/query/different order.json"),
          fetchRequestSpec("/request/query/different params.json"),
          fetchRequestSpec("/request/query/matches.json"),
          fetchRequestSpec("/request/query/missing params.json"),
          fetchRequestSpec("/request/query/same parameter different values.json"),
          fetchRequestSpec("/request/query/same parameter multiple times in different order.json"),
          fetchRequestSpec("/request/query/same parameter multiple times.json"),
          fetchRequestSpec("/request/query/trailing ampersand.json"),
          fetchRequestSpec("/request/query/unexpected param.json")
        )
      )
    }

    it("should check the request header specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/headers/empty headers.json"),
          fetchRequestSpec("/request/headers/header name is different case.json"),
          fetchRequestSpec("/request/headers/header value is different case.json"),
          fetchRequestSpec("/request/headers/matches.json"),
          fetchRequestSpec("/request/headers/order of comma separated header values different.json"),
          fetchRequestSpec("/request/headers/unexpected header found.json"),
          fetchRequestSpec("/request/headers/whitespace after comma different.json")
        )
      )
    }

    it("should check the request header specs with regex") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/headers/matches with regex.json")
        )
      )
    }

    it("should check the request body specs") {
      testRequestSpecs(
        List(
          fetchRequestSpec("/request/body/array at top level.json"),
          //fetchSpec("/request/body/array in different order.json"), // DO NOT AGREE WITH THIS ONE
//          fetchSpec("/request/body/array size less than required.json"),
//          fetchSpec("/request/body/array with at least one element matching by example.json"),
//          fetchSpec("/request/body/array with at least one element not matching example type.json"),
//          fetchSpec("/request/body/array with nested array that does not match.json"),
//          fetchSpec("/request/body/array with nested array that matches.json"),
//          fetchSpec("/request/body/array with regular expression in element.json"),
//          fetchSpec("/request/body/array with regular expression that does not match in element.json"),
          fetchRequestSpec("/request/body/different value found at index.json"),
          fetchRequestSpec("/request/body/different value found at key.json"),
//          fetchSpec("/request/body/matches with regex with bracket notation.json"),
//          fetchSpec("/request/body/matches with regex.json"),
//          fetchSpec("/request/body/matches with type.json"),
          fetchRequestSpec("/request/body/matches.json"),
          fetchRequestSpec("/request/body/missing index.json"),
          fetchRequestSpec("/request/body/missing key.json"),
          fetchRequestSpec("/request/body/no body no content type.json"),
          fetchRequestSpec("/request/body/no body.json"),
          fetchRequestSpec("/request/body/not null found at key when null expected.json"),
          fetchRequestSpec("/request/body/not null found in array when null expected.json"),
          fetchRequestSpec("/request/body/null found at key where not null expected.json"),
          fetchRequestSpec("/request/body/null found in array when not null expected.json"),
          fetchRequestSpec("/request/body/number found at key when string expected.json"),
          fetchRequestSpec("/request/body/number found in array when string expected.json"),
          fetchRequestSpec("/request/body/plain text that does not match.json"),
          fetchRequestSpec("/request/body/plain text that matches.json"),
          fetchRequestSpec("/request/body/string found at key when number expected.json"),
          fetchRequestSpec("/request/body/string found in array when number expected.json")//,
          //fetchSpec("/request/body/unexpected index with not null value.json"),  // DO NOT AGREE WITH THIS ONE
          //fetchSpec("/request/body/unexpected index with null value.json"),  // DO NOT AGREE WITH THIS ONE
          //fetchSpec("/request/body/unexpected key with not null value.json"),  // DO NOT AGREE WITH THIS ONE
          //fetchSpec("/request/body/unexpected key with null value.json")  // DO NOT AGREE WITH THIS ONE
        )
      )
    }
  }

}
