package com.ing.pact.stubber

import java.util.ResourceBundle

trait ErrorStrategy[Error, Data] extends (Seq[Either[Error, Data]] => Seq[Data])


object ErrorStrategy extends Pimpers {
  def printErrorsAndUseGood[T: MessageFormatData, Error, Data](key: String, t: T)(implicit resourceBundle: ResourceBundle): ErrorStrategy[Error, Data] = new ErrorStrategy[Error, Data] {

    def apply(seq: Seq[Either[Error, Data]]) = {
      seq.issues match {
        case Seq() =>
        case issues => issues.printWithTitle(key, t)
      }
      seq.values
    }
  }

  def printErrorsAndAbort[T: MessageFormatData, Error, Data](key: String, t: T)(implicit resourceBundle: ResourceBundle): ErrorStrategy[Error, Data] = new ErrorStrategy[Error, Data] {
    def apply(seq: Seq[Either[Error, Data]]) = {
      seq.issues match {
        case Seq() => seq.values
        case issues => issues.printWithTitle(key, t); Seq()
      }
    }
  }

}
