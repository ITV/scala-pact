package com.itv.scalapactcore.message

import com.itv.scalapact.shared.matchir.IrNodeMatchingRules
import com.itv.scalapact.shared.typeclasses.{IMessageFormat, IPactReader}
import com.itv.scalapact.shared.Message
import com.itv.scalapactcore.common.matching.MessageMatchers.OutcomeAndMessage
import com.itv.scalapactcore.common.matching.{MatchOutcomeFailed, MatchOutcomeSuccess, MessageMatchers}

object MessageStubber {

  def apply[A](
      messages: List[Message],
      results: List[Either[List[String], A]] = List.empty
  )(implicit matchingRules: IrNodeMatchingRules, pactReader: IPactReader): IMessageStubber[A] =
    new IMessageStubber[A] {

      private def messageStub(result: Either[List[String], A]): IMessageStubber[A] =
        MessageStubber.apply(messages, result :: results)
      private def fail(result: String): IMessageStubber[A]       = fail(List(result))
      private def fail(result: List[String]): IMessageStubber[A] = messageStub(Left(result))
      private def success(result: A): IMessageStubber[A]         = messageStub(Right(result))
      private def none: IMessageStubber[A]                       = this

      private def noDescriptionFound(description: String) =
        fail(s"No `$description` found in:\n [ ${messages.map(_.renderAsString).mkString("\n")} \n ]")

      def consume(description: String)(test: Message => A): IMessageStubber[A] =
        messages
          .find(_.description == description)
          .map(test)
          .fold(noDescriptionFound(description)) { r =>
            success(r)
          }

      def publish[T](description: String,
                     actualMessage: T)(implicit messageFormat: IMessageFormat[T]): IMessageStubber[A] =
        messages
          .find(m => m.description == description)
          .fold(noDescriptionFound(description))(
            message =>
              OutcomeAndMessage(
                MessageMatchers.matchSingleMessage(None, message.content, messageFormat.encode(actualMessage)),
                message
              ) match {
                case OutcomeAndMessage(MatchOutcomeSuccess, _) => none
                case outcomeAndMessage @ OutcomeAndMessage(MatchOutcomeFailed(_, _), _) =>
                  fail(
                    MessageMatchers
                      .renderOutcome(Some(outcomeAndMessage), messageFormat.encode(actualMessage), description)
                      .left
                      .get
                  )
            }
          )

      override def currentResult: List[Either[String, A]] = results.map(_.fold(x => Left(x.mkString("")), Right(_)))
    }
}
