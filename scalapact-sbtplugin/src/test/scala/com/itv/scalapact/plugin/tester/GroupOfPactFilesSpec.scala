package com.itv.scalapact.plugin.tester

import java.io.File

import org.mockito.ArgumentCaptor
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

import scala.language.implicitConversions

trait SquashedFixture {
  val rootPath = "src/test/resources/pact-specification-version-2/testcases/samplePacts"

  implicit def toFile(s: String) = new File(s"$rootPath/$s")

  val squashDefn1 = SquashDefn("Bruce-service_Fnord-", List[File](
    "Bruce-service_Fnord-_3faf28db7a1174abd91d513526da030dba4985de.json",
    "Bruce-service_Fnord-_5096e73848781dbbf228d1d3921a0cf960a42f3f.json",
    "Bruce-service_Fnord-_a5a9fb4ec5c84051a20d90419262339b09540517.json"))

  val squashDefn2 = SquashDefn("goggins_thor", List[File](
    "goggins_thor_173fe84b1de4fdb697dbd4bcdc43ebb1275915fb.json",
    "goggins_thor_62e8f8e2614b1d717fea311047e5aa2afb2f3729.json",
    "goggins_thor_ee6caee11c0f665814b760d76f4f61edb1533929.json"
  ))

}

class GroupOfPactFilesSpec extends FlatSpec with Matchers with MockitoSugar with SquashedFixture {

  import org.mockito.Mockito._

  "GroupOfPactFiles" should "creatable from a directory" in {
    GroupOfPactFiles(new File(rootPath)) shouldBe
      Some(GroupOfPactFiles(List[File](
        "Bruce-service_Fnord-_3faf28db7a1174abd91d513526da030dba4985de.json",
        "Bruce-service_Fnord-_5096e73848781dbbf228d1d3921a0cf960a42f3f.json",
        "Bruce-service_Fnord-_a5a9fb4ec5c84051a20d90419262339b09540517.json",
        "goggins_thor_173fe84b1de4fdb697dbd4bcdc43ebb1275915fb.json",
        "goggins_thor_62e8f8e2614b1d717fea311047e5aa2afb2f3729.json",
        "goggins_thor_ee6caee11c0f665814b760d76f4f61edb1533929.json"
      )))
  }

  it should "partition them" in {
    GroupOfPactFiles(new File(rootPath)).get.groupedFileList.sortBy(_.name) shouldBe List(squashDefn1, squashDefn2)
  }

  it should "squash each in turn with the squasher" in {
    val squasher = mock[PactSquasher]
    val captor = ArgumentCaptor.forClass(classOf[SquashDefn])

    GroupOfPactFiles(new File(rootPath)).get.squash(squasher)
    verify(squasher, times(2)).apply(captor.capture())
    import scala.collection.JavaConversions._
    captor.getAllValues.sortBy(_.name) shouldBe List(squashDefn1, squashDefn2)
  }

}
