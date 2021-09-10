package com.itv.scalapact

import com.itv.scalapact.circe14.JsonInstances
import com.itv.scalapact.http4s23.impl.HttpInstances

trait PactVerifySuite extends ScalaPactVerifyDsl with HttpInstances with JsonInstances
