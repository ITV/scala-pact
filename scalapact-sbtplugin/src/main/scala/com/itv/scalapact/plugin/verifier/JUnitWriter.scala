package com.itv.scalapact.plugin.verifier

import java.io.{File, PrintWriter}

import scala.xml.{Elem, NodeBuffer}

object JUnitWriter {

  private val simplifyName: String => String = name =>
    "[^a-zA-Z0-9-]".r.replaceAllIn(name.replace(" ", "-"), "")

  val writePactVerifyResults: String => String => String => Unit = consumer => provider => contents => {
    val dirPath = "target/test-reports"
    val dirFile = new File(dirPath)

    if (!dirFile.exists()) {
      dirFile.mkdir()
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

  def xml(name: String, tests: Int, failures: Int, time: Double, testCases: List[Elem]): NodeBuffer =
    <?xml version='1.0' encoding='UTF-8'?>
      <testsuite hostname="" name={name} tests={"\"" + tests + "\""} errors="0" failures={"\"" + failures + "\""} time={"\"" + time + "\""}>
        <properties></properties>
        {testCases}
        <system-out></system-out>
        <system-err></system-err>
      </testsuite>

//  val xml =
//    <?xml version='1.0' encoding='UTF-8'?>
//      <testsuite hostname="Davids-MacBook-Pro.local" name="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" tests="12" errors="0" failures="1" time="0.716">
//        <properties>
//          <property name="jline.esc.timeout" value="0"/>
//          <property name="java.runtime.name" value="Java(TM) SE Runtime Environment"/>
//          <property name="sun.boot.library.path" value="/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib"/>
//          <property name="java.vm.version" value="25.5-b02"/>
//          <property name="user.country.format" value="GB"/>
//          <property name="gopherProxySet" value="false"/>
//          <property name="java.vm.vendor" value="Oracle Corporation"/>
//          <property name="java.vendor.url" value="http://java.oracle.com/"/>
//          <property name="path.separator" value=":"/>
//          <property name="java.vm.name" value="Java HotSpot(TM) 64-Bit Server VM"/>
//          <property name="file.encoding.pkg" value="sun.io"/>
//          <property name="user.country" value="US"/>
//          <property name="sun.java.launcher" value="SUN_STANDARD"/>
//          <property name="sun.os.patch.level" value="unknown"/>
//          <property name="java.vm.specification.name" value="Java Virtual Machine Specification"/>
//          <property name="user.dir" value="/Users/dave/repos/scalapact/scalapact-sbtplugin"/>
//          <property name="java.runtime.version" value="1.8.0_05-b13"/>
//          <property name="java.awt.graphicsenv" value="sun.awt.CGraphicsEnvironment"/>
//          <property name="java.endorsed.dirs" value="/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/endorsed"/>
//          <property name="os.arch" value="x86_64"/>
//          <property name="java.io.tmpdir" value="/var/folders/c0/5fdxn7rs37n74m2k7jt1c06w0000gn/T/"/>
//          <property name="line.separator" value=""/>
//          <property name="java.vm.specification.vendor" value="Oracle Corporation"/>
//          <property name="os.name" value="Mac OS X"/>
//          <property name="sun.jnu.encoding" value="UTF-8"/>
//          <property name="java.library.path" value="/Users/dave/Library/Java/Extensions:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java:."/>
//          <property name="java.specification.name" value="Java Platform API Specification"/>
//          <property name="java.class.version" value="52.0"/>
//          <property name="sun.management.compiler" value="HotSpot 64-Bit Tiered Compilers"/>
//          <property name="os.version" value="10.11.2"/>
//          <property name="user.home" value="/Users/dave"/>
//          <property name="user.timezone" value="Europe/London"/>
//          <property name="java.awt.printerjob" value="sun.lwawt.macosx.CPrinterJob"/>
//          <property name="file.encoding" value="UTF-8"/>
//          <property name="java.specification.version" value="1.8"/>
//          <property name="java.class.path" value="/usr/local/Cellar/sbt/0.13.5/libexec/sbt-launch.jar"/>
//          <property name="user.name" value="dave"/>
//          <property name="jline.shutdownhook" value="false"/>
//          <property name="java.vm.specification.version" value="1.8"/>
//          <property name="sun.java.command" value="/usr/local/Cellar/sbt/0.13.5/libexec/sbt-launch.jar test-only com.itv.scalapact.plugin.stubber.InteractionMatchersSpec"/>
//          <property name="java.home" value="/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre"/>
//          <property name="sun.arch.data.model" value="64"/>
//          <property name="user.language" value="en"/>
//          <property name="java.specification.vendor" value="Oracle Corporation"/>
//          <property name="awt.toolkit" value="sun.lwawt.macosx.LWCToolkit"/>
//          <property name="java.vm.info" value="mixed mode"/>
//          <property name="java.version" value="1.8.0_05"/>
//          <property name="java.ext.dirs" value="/Users/dave/Library/Java/Extensions:/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/ext:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java"/>
//          <property name="sun.boot.class.path" value="/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/sunrsasign.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/jre/classes"/>
//          <property name="java.vendor" value="Oracle Corporation"/>
//          <property name="file.separator" value="/"/>
//          <property name="java.vendor.url.bug" value="http://bugreport.sun.com/bugreport/"/>
//          <property name="sun.io.unicode.encoding" value="UnicodeBig"/>
//          <property name="sun.cpu.endian" value="little"/>
//          <property name="sun.cpu.isalist" value=""/>
//        </properties>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching status codes should be able to match status codes" time="0.022">
//        </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching methods should be able to match methods" time="0.002">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching headers should be able to match simple headers" time="0.006">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching headers should be able to spot mismatched headers" time="0.0">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching headers should be able to find the expected subset in a collection of headers" time="0.002">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching headers should be able to handle a more complex case" time="0.0">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching headers should be able to handle a more complex case where it needs to match" time="0.001">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching paths should be able to match paths" time="0.023">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="a failing test should fail" time="0.006">
//        <failure message="Uh oh..." type="org.scalatest.exceptions.TestFailedException">org.scalatest.exceptions.TestFailedException: Uh oh...
//          at org.scalatest.Assertions$class.newAssertionFailedException(Assertions.scala:495)
//          at org.scalatest.FunSpec.newAssertionFailedException(FunSpec.scala:1626)
//          at org.scalatest.Assertions$class.fail(Assertions.scala:1328)
//          at org.scalatest.FunSpec.fail(FunSpec.scala:1626)
//          at com.itv.scalapact.plugin.stubber.InteractionMatchersSpec$$anonfun$5$$anonfun$apply$mcV$sp$22.apply(InteractionMatchersSpec.scala:135)
//          at com.itv.scalapact.plugin.stubber.InteractionMatchersSpec$$anonfun$5$$anonfun$apply$mcV$sp$22.apply(InteractionMatchersSpec.scala:135)
//          at org.scalatest.Transformer$$anonfun$apply$1.apply$mcV$sp(Transformer.scala:22)
//          at org.scalatest.OutcomeOf$class.outcomeOf(OutcomeOf.scala:85)
//          at org.scalatest.OutcomeOf$.outcomeOf(OutcomeOf.scala:104)
//          at org.scalatest.Transformer.apply(Transformer.scala:22)
//          at org.scalatest.Transformer.apply(Transformer.scala:20)
//          at org.scalatest.FunSpecLike$$anon$1.apply(FunSpecLike.scala:422)
//          at org.scalatest.Suite$class.withFixture(Suite.scala:1122)
//          at org.scalatest.FunSpec.withFixture(FunSpec.scala:1626)
//          at org.scalatest.FunSpecLike$class.invokeWithFixture$1(FunSpecLike.scala:419)
//          at org.scalatest.FunSpecLike$$anonfun$runTest$1.apply(FunSpecLike.scala:431)
//          at org.scalatest.FunSpecLike$$anonfun$runTest$1.apply(FunSpecLike.scala:431)
//          at org.scalatest.SuperEngine.runTestImpl(Engine.scala:306)
//          at org.scalatest.FunSpecLike$class.runTest(FunSpecLike.scala:431)
//          at org.scalatest.FunSpec.runTest(FunSpec.scala:1626)
//          at org.scalatest.FunSpecLike$$anonfun$runTests$1.apply(FunSpecLike.scala:464)
//          at org.scalatest.FunSpecLike$$anonfun$runTests$1.apply(FunSpecLike.scala:464)
//          at org.scalatest.SuperEngine$$anonfun$traverseSubNodes$1$1.apply(Engine.scala:413)
//          at org.scalatest.SuperEngine$$anonfun$traverseSubNodes$1$1.apply(Engine.scala:401)
//          at scala.collection.immutable.List.foreach(List.scala:318)
//          at org.scalatest.SuperEngine.traverseSubNodes$1(Engine.scala:401)
//          at org.scalatest.SuperEngine.org$scalatest$SuperEngine$$runTestsInBranch(Engine.scala:390)
//          at org.scalatest.SuperEngine$$anonfun$traverseSubNodes$1$1.apply(Engine.scala:427)
//          at org.scalatest.SuperEngine$$anonfun$traverseSubNodes$1$1.apply(Engine.scala:401)
//          at scala.collection.immutable.List.foreach(List.scala:318)
//          at org.scalatest.SuperEngine.traverseSubNodes$1(Engine.scala:401)
//          at org.scalatest.SuperEngine.org$scalatest$SuperEngine$$runTestsInBranch(Engine.scala:396)
//          at org.scalatest.SuperEngine.runTestsImpl(Engine.scala:483)
//          at org.scalatest.FunSpecLike$class.runTests(FunSpecLike.scala:464)
//          at org.scalatest.FunSpec.runTests(FunSpec.scala:1626)
//          at org.scalatest.Suite$class.run(Suite.scala:1424)
//          at org.scalatest.FunSpec.org$scalatest$FunSpecLike$$super$run(FunSpec.scala:1626)
//          at org.scalatest.FunSpecLike$$anonfun$run$1.apply(FunSpecLike.scala:468)
//          at org.scalatest.FunSpecLike$$anonfun$run$1.apply(FunSpecLike.scala:468)
//          at org.scalatest.SuperEngine.runImpl(Engine.scala:545)
//          at org.scalatest.FunSpecLike$class.run(FunSpecLike.scala:468)
//          at org.scalatest.FunSpec.run(FunSpec.scala:1626)
//          at org.scalatest.tools.Framework.org$scalatest$tools$Framework$$runSuite(Framework.scala:357)
//          at org.scalatest.tools.Framework$ScalaTestTask.execute(Framework.scala:502)
//          at sbt.TestRunner.runTest$1(TestFramework.scala:76)
//          at sbt.TestRunner.run(TestFramework.scala:85)
//          at sbt.TestFramework$$anon$2$$anonfun$$init$$1$$anonfun$apply$8.apply(TestFramework.scala:202)
//          at sbt.TestFramework$$anon$2$$anonfun$$init$$1$$anonfun$apply$8.apply(TestFramework.scala:202)
//          at sbt.TestFramework$.sbt$TestFramework$$withContextLoader(TestFramework.scala:185)
//          at sbt.TestFramework$$anon$2$$anonfun$$init$$1.apply(TestFramework.scala:202)
//          at sbt.TestFramework$$anon$2$$anonfun$$init$$1.apply(TestFramework.scala:202)
//          at sbt.TestFunction.apply(TestFramework.scala:207)
//          at sbt.Tests$$anonfun$9.apply(Tests.scala:216)
//          at sbt.Tests$$anonfun$9.apply(Tests.scala:216)
//          at sbt.std.Transform$$anon$3$$anonfun$apply$2.apply(System.scala:44)
//          at sbt.std.Transform$$anon$3$$anonfun$apply$2.apply(System.scala:44)
//          at sbt.std.Transform$$anon$4.work(System.scala:63)
//          at sbt.Execute$$anonfun$submit$1$$anonfun$apply$1.apply(Execute.scala:228)
//          at sbt.Execute$$anonfun$submit$1$$anonfun$apply$1.apply(Execute.scala:228)
//          at sbt.ErrorHandling$.wideConvert(ErrorHandling.scala:17)
//          at sbt.Execute.work(Execute.scala:237)
//          at sbt.Execute$$anonfun$submit$1.apply(Execute.scala:228)
//          at sbt.Execute$$anonfun$submit$1.apply(Execute.scala:228)
//          at sbt.ConcurrentRestrictions$$anon$4$$anonfun$1.apply(ConcurrentRestrictions.scala:159)
//          at sbt.CompletionService$$anon$2.call(CompletionService.scala:28)
//          at java.util.concurrent.FutureTask.run(FutureTask.java:266)
//          at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
//          at java.util.concurrent.FutureTask.run(FutureTask.java:266)
//          at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
//          at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
//          at java.lang.Thread.run(Thread.java:745)
//        </failure>
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching bodies should be able to match plain text bodies" time="0.003">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching bodies should be able to handle missing bodies and no expectation of bodies" time="0.003">
//      </testcase>
//        <testcase classname="com.itv.scalapact.plugin.stubber.InteractionMatchersSpec" name="Matching bodies should be able to match json bodies" time="0.648">
//      </testcase>
//        <system-out></system-out>
//        <system-err></system-err>
//      </testsuite>

}