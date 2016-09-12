package com.itv.scalapactcore.pactspec

import com.itv.scalapactcore.pactspec.util.{NonStrictOnly, PactSpecTester}

class ResponseV1Spec extends PactSpecTester {

  val pactSpecVersion = "1"

  describe("Exercising response V" + pactSpecVersion + " Pact Specification match tests") {

    it("should check the response status specs") {
      testResponseSpecs(
        List(
          fetchResponseSpec("/response/status/different status.json")(NonStrictOnly),
          fetchResponseSpec("/response/status/matches.json")(NonStrictOnly)
        )
      )
    }

    it("should check the response header specs") {
      testResponseSpecs(
        List(
          fetchResponseSpec("/response/headers/empty headers.json")(NonStrictOnly),
          fetchResponseSpec("/response/headers/header name is different case.json")(NonStrictOnly),
          fetchResponseSpec("/response/headers/header value is different case.json")(NonStrictOnly),
          fetchResponseSpec("/response/headers/matches.json")(NonStrictOnly),
          fetchResponseSpec("/response/headers/order of comma separated header values different.json")(NonStrictOnly),
          fetchResponseSpec("/response/headers/unexpected header found.json")(NonStrictOnly),
          fetchResponseSpec("/response/headers/whitespace after comma different.json")(NonStrictOnly)
        )
      )
    }

    it("should check the response body specs") {
      testResponseSpecs(
        List(
          fetchResponseSpec("/response/body/array in different order.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/deeply nested objects.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/different value found at index.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/different value found at key.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/keys out of order match.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/matches.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/missing index.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/missing key.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/not null found at key when null expected.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/not null found in array when null expected.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/null found at key where not null expected.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/null found in array when not null expected.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/number found at key when string expected.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/number found in array when string expected.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/objects in array first matches.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/objects in array no matches.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/objects in array second matches.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/plain text that does not match.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/plain text that matches.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/property name is different case.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/string found at key when number expected.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/string found in array when number expected.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/unexpected index with not null value.json")(NonStrictOnly), // DO NOT AGREE WITH THIS ONE
          fetchResponseSpec("/response/body/unexpected index with null value.json")(NonStrictOnly), // DO NOT AGREE WITH THIS ONE
          fetchResponseSpec("/response/body/unexpected key with not null value.json")(NonStrictOnly),
          fetchResponseSpec("/response/body/unexpected key with null value.json")(NonStrictOnly)
        )
      )
    }

  }

}
