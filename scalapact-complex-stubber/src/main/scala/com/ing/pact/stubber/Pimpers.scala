package com.ing.pact.stubber

import java.io.{File, FileFilter}
import java.text.MessageFormat
import java.util.ResourceBundle
import javax.net.ssl.SSLContext

import com.itv.scalapact.shared.{PactLogger, SSLContextData, SslContextMap}
import com.typesafe.config.Config

import scala.collection.JavaConverters._

trait FromConfig[T] {
  def apply(config: Config): T
}

trait FromConfigWithKey[T] {
  def apply(name: String, config: Config): T
}

object FromConfig {

  implicit object FromConfigForSslContext extends FromConfig[SSLContextData] {
    override def apply(config: Config): SSLContextData = SSLContextData(config.getString("keystore"), config.getString("keystore-password"), config.getString("truststore"), config.getString("truststore-password"))
  }

}

trait MessageFormatData[T] extends (T => Seq[String])

object MessageFormatData extends Pimpers {

  def apply[T](t: T)(implicit messageFormatData: MessageFormatData[T]) = messageFormatData(t)

  implicit object MessageFormatDataForUnit extends MessageFormatData[Unit] {
    override def apply(v1: Unit): Seq[String] = Seq()
  }

  implicit object MessageFormatDataForString extends MessageFormatData[String] {
    override def apply(v1: String): Seq[String] = Seq(v1)
  }

  implicit def messageFormatDataForOption[T](implicit mf: MessageFormatData[T]): MessageFormatData[Option[T]] = new MessageFormatData[Option[T]] {
    override def apply(v1: Option[T]): Seq[String] = Seq(v1.fold("")(_.toString))
  }
  implicit def messageFormatDataForTuple2[L, R](implicit mfl: MessageFormatData[L], mfr: MessageFormatData[R]): MessageFormatData[(L, R)] = new MessageFormatData[(L, R)] {
    override def apply(v1: (L, R)): Seq[String] = mfl(v1._1) ++ mfr(v1._2)
  }
  implicit def messageFormatDataForTuple3[L, M, R](implicit mfl: MessageFormatData[L], mfm: MessageFormatData[M], mfr: MessageFormatData[R]): MessageFormatData[(L, M, R)] = new MessageFormatData[(L, M, R)] {
    override def apply(v1: (L, M, R)): Seq[String] = mfl(v1._1) ++ mfm(v1._2) ++ mfr(v1._3)
  }

  implicit def messageFormatDataForSeq[T]: MessageFormatData[Seq[T]]  = new MessageFormatData[Seq[T]] {
    override def apply(v1: Seq[T]): Seq[String] = Seq(v1.size.toString)
  }

  implicit object messageFormatDataForFile extends MessageFormatData[File] {
    override def apply(v1: File): Seq[String] = Seq(v1.getAbsolutePath)
  }

}

trait Pimpers {

  implicit class AnyPimper[T](t: T) {
    def ==>[T1](fn: T => T1): T1 = fn(t)

  }

  implicit class StringPimper(s: String)(implicit resources: ResourceBundle) {
    def fromBundle[T](t: T)(implicit messageFormatData: MessageFormatData[T]): String = MessageFormat.format(resources.getString(s), messageFormatData(t): _*)
    def printlnFromBundle[T: MessageFormatData](t: T)(implicit pactLogger: PactLogger): Unit = pactLogger.message(fromBundle(t))
  }

  implicit class FnPimper[From, To](fn: From => To) {
    def ===>[T1](fn2: To => T1): From => T1 = fn andThen fn2
    def =^>(fn2: To => Unit): From => To = { from: From =>
      val result = fn(from)
      fn2(result)
      result
    }
  }

  implicit class BooleanPimper(b: Boolean) {
    def toOption[X](block: => X): Option[X] = if (b) Some(block) else None
  }

  implicit class OptionPimper[T](o: Option[T]) {
    def asString(falseS: => String, trueS: T => String): String = o.fold(falseS)(trueS)
  }

  implicit class SeqPimper[T](seq: Seq[T]) {
    def ===>[T1](fn: T => T1): Seq[T1] = seq.map(fn)

    /** This is for those times when you want the result to be fed into a chain for functions, but also want it available for downstream methods */
    def mapWith[T1](fn: T => T => T1): Seq[T1] = seq.map(t => fn(t)(t))
    def printWithTitle[TitleObject: MessageFormatData](title: String, titleObject: TitleObject)(implicit resourceBundle: ResourceBundle): Seq[T] = {
      if (seq.nonEmpty) title.printlnFromBundle(titleObject)
      seq.foreach(println)
      seq
    }
    def ifNotEmpty(block: => Unit): Seq[T] = {
      if (seq.nonEmpty) block
      seq
    }
  }

  implicit class SeqEitherPimper[L, R](seq: Seq[Either[L, R]]) {
    def issues: Seq[L] = seq.collect { case Left(l) => l }
    def values: Seq[R] = seq.collect { case Right(r) => r }
    def handleErrors(implicit errorStrategy: ErrorStrategy[L, R]): Seq[R] = errorStrategy(seq)
  }


  implicit class ConfigPimper(config: Config) {
    def get[A](name: String)(implicit fromConfig: FromConfig[A]): A = fromConfig(config.getConfig(name))
    def getOption[A](name: String)(implicit fromConfig: FromConfig[A]): Option[A] = if (config.hasPath(name)) Some(fromConfig(config.getConfig(name))) else None
    def mapList[A](name: String)(implicit fromConfig: FromConfigWithKey[A]): List[A] = config.getObject(name).keySet().asScala.toList.sorted.map(key => fromConfig(key, config.getConfig(name).getConfig(key)))
    def getFiles(name: String)(filenameFilter: FileFilter): List[File] = {
      val fileName = config.getString(name)
      if (fileName == null) throw new NullPointerException(s"Cannot load file $name")
      val directory = new File(fileName)
      if (!directory.isDirectory) throw new IllegalArgumentException(s"Filename $name is $fileName and that isn't a directory")
      directory.listFiles(filenameFilter).toList
    }
  }

  def fromConfig(name: String)(config: Config): (Config, String) = (config, name)
  def makeListFromConfig[A: FromConfigWithKey](key: String)(config: Config): Seq[A] = config.mapList(key)

}
