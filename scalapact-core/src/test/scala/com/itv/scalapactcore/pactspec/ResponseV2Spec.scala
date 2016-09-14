package com.itv.scalapactcore.pactspec

import com.itv.scalapactcore.pactspec.util.{PactSpecTester, StrictAndNonStrict, StrictOnly}

class ResponseV2Spec extends PactSpecTester {

  val pactSpecVersion = "2"

  describe("Exercising response V" + pactSpecVersion + " Pact Specification match tests") {

    it("should check the response status specs") {
      testResponseSpecs(
        List(
          fetchResponseSpec("/response/status/different status.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/status/matches.json")(StrictAndNonStrict)
        )
      )
    }

    it("should check the response header specs") {
      testResponseSpecs(
        List(
          fetchResponseSpec("/response/headers/empty headers.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/headers/header name is different case.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/headers/header value is different case.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/headers/matches.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/headers/order of comma separated header values different.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/headers/unexpected header found.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/headers/whitespace after comma different.json")(StrictAndNonStrict)
        )
      )
    }

    it("should check the response header specs with regex") {
      testResponseSpecs(
        List(
          fetchResponseSpec("/response/headers/matches with regex.json")(StrictAndNonStrict)
        )
      )
    }

    it("should check the response body specs") {
      testResponseSpecs(
        List(
//          fetchResponseSpec("/response/body/additional property with type matcher.json")(StrictAndNonStrict),
//          fetchResponseSpec("/response/body/array at top level with matchers.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/array at top level.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/array in different order.json")(StrictOnly), // DO NOT AGREE WITH THIS ONE
//          fetchResponseSpec("/response/body/array with regex matcher.json")(StrictAndNonStrict),
//          fetchResponseSpec("/response/body/array with type matcher mismatch.json")(StrictAndNonStrict),
//          fetchResponseSpec("/response/body/array with type matcher.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/deeply nested objects.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/different value found at index.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/different value found at key.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/keys out of order match.json")(StrictAndNonStrict),
//          fetchResponseSpec("/response/body/matches with regex.json")(StrictAndNonStrict),
//          fetchResponseSpec("/response/body/matches with type.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/matches.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/missing body.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/missing index.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/missing key.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/no body no content type.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/not null found at key when null expected.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/not null found in array when null expected.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/null found at key where not null expected.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/null found in array when not null expected.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/number found at key when string expected.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/number found in array when string expected.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/objects in array first matches.json")(StrictOnly),
          fetchResponseSpec("/response/body/objects in array no matches.json")(StrictOnly),
          fetchResponseSpec("/response/body/objects in array second matches.json")(StrictOnly),
//          fetchResponseSpec("/response/body/objects in array type matching.json")(StrictAndNonStrict),
//          fetchResponseSpec("/response/body/objects in array with type mismatching.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/plain text that does not match.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/plain text that matches.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/property name is different case.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/string found at key when number expected.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/string found in array when number expected.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/unexpected index with not null value.json")(StrictOnly), // DO NOT AGREE WITH THIS ONE
          fetchResponseSpec("/response/body/unexpected index with null value.json")(StrictOnly), // DO NOT AGREE WITH THIS ONE
          fetchResponseSpec("/response/body/unexpected key with not null value.json")(StrictAndNonStrict),
          fetchResponseSpec("/response/body/unexpected key with null value.json")(StrictAndNonStrict)
        )
      )
    }
  }

}
