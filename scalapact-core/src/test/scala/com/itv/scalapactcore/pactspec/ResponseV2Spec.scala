package com.itv.scalapactcore.pactspec

import com.itv.scalapactcore.pactspec.util.{NonStrictOnly, PactSpecTester, StrictAndNonStrict, StrictOnly}

class ResponseV2Spec extends PactSpecTester {

  val pactSpecVersion = "2"

  describe("Exercising response V" + pactSpecVersion + " Pact Specification match tests") {

    // it("should check the response status specs") {
    //   testResponseSpecs(
    //     List(
    //       fetchResponseSpec("/response/status/different status.json")(StrictAndNonStrict),
    //       fetchResponseSpec("/response/status/matches.json")(StrictAndNonStrict)
    //     )
    //   )
    // }
    //
    // it("should check the response header specs") {
    //   testResponseSpecs(
    //     List(
    //       fetchResponseSpec("/response/headers/empty headers.json")(StrictAndNonStrict),
    //       fetchResponseSpec("/response/headers/header name is different case.json")(StrictAndNonStrict),
    //       fetchResponseSpec("/response/headers/header value is different case.json")(StrictAndNonStrict),
    //       fetchResponseSpec("/response/headers/matches with regex.json")(StrictAndNonStrict),
    //       fetchResponseSpec("/response/headers/matches.json")(StrictAndNonStrict),
    //       fetchResponseSpec("/response/headers/order of comma separated header values different.json")(StrictAndNonStrict),
    //       fetchResponseSpec("/response/headers/unexpected header found.json")(StrictAndNonStrict),
    //       fetchResponseSpec("/response/headers/whitespace after comma different.json")(StrictAndNonStrict)
    //     )
    //   )
    // }

    it("should check the response body specs") {
      testResponseSpecs(
        List(
          // fetchResponseSpec("/response/body/additional property with type matcher.json")(StrictAndNonStrict),

          //--- Not implemented
          // fetchResponseSpec("/response/body/array at top level with matchers xml.json")(StrictAndNonStrict),
          //--- Not implemented

          // fetchResponseSpec("/response/body/array at top level with matchers.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/array at top level xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/array at top level.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/array in different order xml.json")(StrictOnly),
          // fetchResponseSpec("/response/body/array in different order.json")(StrictOnly),
          // fetchResponseSpec("/response/body/array with regex matcher xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/array with regex matcher.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/array with type matcher mismatch xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/array with type matcher mismatch.json")(StrictAndNonStrict),

          //--- Not implemented
          // fetchResponseSpec("/response/body/array with type matcher xml.json")(StrictAndNonStrict)//,
          //--- Not implemented

          // fetchResponseSpec("/response/body/array with type matcher.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/deeply nested objects xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/deeply nested objects.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/different value found at index xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/different value found at index.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/different value found at key xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/different value found at key.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/empty body no content type.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/empty body.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/keys out of order match xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/keys out of order match.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/matches with regex xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/matches with regex.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/matches with type.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/matches xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/matches.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/missing body found when empty expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/missing body no content type.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/missing body xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/missing body.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/missing index xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/missing index.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/missing key xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/missing key.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/no body no content type xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/no body no content type.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/non empty body found when empty expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/not null found at key when null expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/not null found in array when null expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/null body no content type.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/null body.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/null found at key where not null expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/null found in array when not null expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/number found at key when string expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/number found in array when string expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/objects in array first matches xml.json")(StrictOnly),
          // fetchResponseSpec("/response/body/objects in array first matches.json")(StrictOnly),
          // fetchResponseSpec("/response/body/objects in array no matches xml.json")(StrictOnly),
          // fetchResponseSpec("/response/body/objects in array no matches.json")(StrictOnly),
          // fetchResponseSpec("/response/body/objects in array second matches xml.json")(StrictOnly),
          // fetchResponseSpec("/response/body/objects in array second matches.json")(StrictOnly),

          //--- Not implemented
          fetchResponseSpec("/response/body/objects in array type matching xml.json")(StrictAndNonStrict)//,
          //--- Not implemented

          // fetchResponseSpec("/response/body/objects in array type matching.json")(NonStrictOnly),
          // fetchResponseSpec("/response/body/objects in array with type mismatching xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/objects in array with type mismatching.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/plain text that does not match.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/plain text that matches.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/property name is different case xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/property name is different case.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/string found at key when number expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/string found in array when number expected.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/unexpected index with missing value xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/unexpected index with non-empty value xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/unexpected index with not null value.json")(StrictOnly),
          // fetchResponseSpec("/response/body/unexpected index with null value.json")(StrictOnly),
          // fetchResponseSpec("/response/body/unexpected key with empty value xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/unexpected key with non-empty value xml.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/unexpected key with null value.json")(StrictAndNonStrict),
          // fetchResponseSpec("/response/body/value found in array when empty expected xml.json")(StrictAndNonStrict)
        )
      )
    }
  }

}
