package com.itv.scalapact.plugin

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import com.itv.scalapact.plugin.tester.{GroupOfPactFiles, PactSquasher, ScalaPactTestCommand, SquashedFixture}
import com.itv.scalapactcore.ScalaPactReader
import org.http4s.CacheDirective.public
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.io.Source

trait PactFileIntegrationFixture {
  val pactDir = new File("target/pacts")
  val targetPath: Path = pactDir.toPath
  val sourcePath: Path = new File("src/test/resources/pact-specification-version-2/testcases/samplePacts").toPath

  // source
  def copyFilesFromMasterCopyToBeingUsed = Files.walkFileTree(sourcePath, new SimpleFileVisitor[Path]() {
    override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
      Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)))
      FileVisitResult.CONTINUE
    }

    override def visitFile(file: Path, attrs: BasicFileAttributes) = {
      Files.copy(file, targetPath.resolve(sourcePath.relativize(file)))
      FileVisitResult.CONTINUE;
    }
  })

  def nukePacts = pactDir.listFiles().foreach (_.delete)

}

class PactTestIntegrationSpec extends FlatSpec with Matchers with PactFileIntegrationFixture with BeforeAndAfter {

  before {
    nukePacts
    copyFilesFromMasterCopyToBeingUsed
  }
  lazy val squasher = PactSquasher()

  def fileToPact = (file: File) => ScalaPactReader.jsonStringToPact(Source.fromFile(file).getLines().mkString("\n")).toOption.get

  def removeWhiteSpace(s: Any) = s.toString.replaceAll("\\s+", "")

  "The pact test " should "aggregate files together into one file" in {
    val brucePacts = pactDir.listFiles.filter(_.getName.startsWith("Bruce")).map(fileToPact)
    val gogginsPacts = pactDir.listFiles.filter(_.getName.startsWith("goggins_thor")).map(fileToPact)

    ScalaPactTestCommand.doPactTest

    val Array(bruceFnord, gogginsThor) = pactDir.listFiles.filter(_.getName.endsWith(".json")).sortBy(_.getName)
    bruceFnord.getName shouldBe "Bruce-service_Fnord-.json"
    gogginsThor.getName shouldBe "goggins_thor.json"

    removeWhiteSpace(fileToPact(bruceFnord)) shouldBe removeWhiteSpace(brucePacts.reduce((p1, p2) => p1.copy(interactions = p1.interactions ++ p2.interactions)))
    removeWhiteSpace(fileToPact(gogginsThor)) shouldBe removeWhiteSpace(gogginsPacts.reduce((p1, p2) => p1.copy(interactions = p1.interactions ++ p2.interactions)))
  }
}
