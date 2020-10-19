package com.itv.scalapact

import com.itv.scalapact.circe13.JsonInstances
import com.itv.scalapact.http4s21.impl.HttpInstances

trait PactVerifySuite extends ScalaPactVerifyDsl with HttpInstances with JsonInstances
