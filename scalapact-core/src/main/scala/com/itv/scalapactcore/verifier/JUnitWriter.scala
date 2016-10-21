package com.itv.scalapactcore.verifier

import java.io.{File, PrintWriter}

import scala.xml.Elem

object JUnitWriter {

  private val simplifyName: String => String = name =>
    "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")

  val writePactVerifyResults: String => String => String => Unit = consumer => provider => contents => {
    val dirPath = "target/test-reports"
    val dirFile = new File(dirPath)

    if (!dirFile.exists()) {
      dirFile.mkdirs()
    }

    val relativePath = dirPath + "/" + simplifyName(consumer) + "_" + simplifyName(provider) + ".xml"
    val file = new File(relativePath)

    if (file.exists()) {
      file.delete()
    }

    file.createNewFile()

    new PrintWriter(relativePath) {
      write(contents)
      close()
    }

    ()
  }

}

object JUnitXmlBuilder {

  def testCasePass(name: String): Elem =
    <testcase classname="" name={name} time="0.0"></testcase>

  def testCaseFail(name: String, message: String): Elem =
    <testcase classname="" name={name} time="0.0">
      <failure message={message} type="Pact validation error">{message}</failure>
    </testcase>

  def xml(name: String, tests: Int, failures: Int, time: Double, testCases: List[Elem]): String = {
    """<?xml version='1.0' encoding='UTF-8'?>""" +
      <testsuite hostname="" name={name} tests={tests.toString} errors="0" failures={failures.toString} time={time.toString}>
        <properties></properties>{testCases}<system-out></system-out>
        <system-err></system-err>
      </testsuite>.toString()
  }

}
