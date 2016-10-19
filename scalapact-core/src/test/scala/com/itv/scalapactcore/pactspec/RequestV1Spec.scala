package com.itv.scalapactcore.pactspec

import com.itv.scalapactcore.pactspec.util.PactSpecTester

class RequestV1Spec extends PactSpecTester {

  val pactSpecVersion = "1"

//  describe("Exercising response V" + pactSpecVersion + " Pact Specification match tests") {
//
//    it("should check the request method specs") {
//      testRequestSpecs(
//        List(
//          fetchRequestSpec("/request/method/different method.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/method/matches.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/method/method is different case.json")(StrictAndNonStrict)
//        )
//      )
//    }
//
//    it("should check the request path specs") {
//      testRequestSpecs(
//        List(
//          fetchRequestSpec("/request/path/empty path found when forward slash expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/path/forward slash found when empty path expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/path/incorrect path.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/path/matches.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/path/missing trailing slash in path.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/path/unexpected trailing slash in path.json")(StrictAndNonStrict)
//        )
//      )
//    }
//
//    it("should check the request query specs") {
//      testRequestSpecs(
//        List(
//          fetchRequestSpec("/request/query/different param order.json")(StrictOnly),
//          fetchRequestSpec("/request/query/different param values.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/query/matches with equals in the query value.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/query/matches.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/query/trailing amperand.json")(StrictOnly)
//        )
//      )
//    }
//
//    it("should check the request header specs") {
//      testRequestSpecs(
//        List(
//          fetchRequestSpec("/request/headers/empty headers.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/headers/header name is different case.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/headers/header value is different case.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/headers/matches.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/headers/order of comma separated header values different.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/headers/unexpected header found.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/headers/whitespace after comma different.json")(StrictAndNonStrict)
//        )
//      )
//    }
//
//    it("should check the request body specs") {
//      testRequestSpecs(
//        List(
//          fetchRequestSpec("/request/body/array in different order.json")(StrictOnly),
//          fetchRequestSpec("/request/body/different value found at index.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/different value found at key.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/matches.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/missing index.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/missing key.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/not null found at key when null expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/not null found in array when null expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/null found at key where not null expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/null found in array when not null expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/number found at key when string expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/number found in array when string expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/plain text that does not match.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/plain text that matches.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/string found at key when number expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/string found in array when number expected.json")(StrictAndNonStrict),
//          fetchRequestSpec("/request/body/unexpected index with not null value.json")(StrictOnly),
//          fetchRequestSpec("/request/body/unexpected index with null value.json")(StrictOnly),
//          fetchRequestSpec("/request/body/unexpected key with not null value.json")(StrictOnly),
//          fetchRequestSpec("/request/body/unexpected key with null value.json")(StrictOnly)
//        )
//      )
//    }
//  }

}
