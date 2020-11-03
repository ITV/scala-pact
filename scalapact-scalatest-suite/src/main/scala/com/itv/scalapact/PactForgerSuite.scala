package com.itv.scalapact

import com.itv.scalapact.circe13.JsonInstances
import com.itv.scalapact.http4s21.impl.{HttpInstances, PactStubber}
import com.itv.scalapact.shared.http.{HttpMethod, IScalaPactHttpClient, SimpleRequest}
import com.itv.scalapact.shared.{IInteractionManager, IPactStubber}
import com.itv.scalapactcore.common.stubber.InteractionManager
import org.scalatest._

trait PactForgerSuite extends ScalaPactForgerDslMixin with HttpInstances with JsonInstances with SuiteMixin {
  self: Suite =>

  implicit val stubber: IPactStubber = new PactStubber(new InteractionManager)

  abstract override def run(testName: Option[String], args: Args): Status = {
    if (expectedTestCount(args.filter) == 0) {
      new CompositeStatus(Set.empty)
    } else {
      stubber.start(???, ???, ???)
      try {
        super.run(testName, args)
      } finally {
        stubber.shutdown()
      }
    }
  }

  abstract protected override def runTest(testName: String, args: Args): Status = {
    @volatile var testCalled = false
    @volatile var afterTestCalled = false

    try {
      testCalled = true
      val status = super.runTest(testName, args)

      afterTestCalled = true
      if (!status.succeeds()) {
        afterTest(Some(new RuntimeException("Test failed")))
      } else {
        afterTest(None)
      }

      status
    }
    catch {
      case e: Throwable =>
        if (testCalled && !afterTestCalled) {
          afterTestCalled = true
          afterTest(Some(e))
        }

        throw e
    }
  }

}
