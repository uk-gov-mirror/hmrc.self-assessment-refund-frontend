/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney

import cats.syntax.eq._
import org.apache.commons.lang3.StringUtils
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormError, Mapping}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.BankAccountDetailsController.BankAccount

object BankAccountDetailsForm {
  val validAccountNameRegex = """^[a-zA-Z0-9!@#$%&() \-`\.\'+,\/\"]{1,60}$"""
  val validSortCodeRegex = """^[0-9- ]+$"""
  val validRollNumberRegex = """^[a-zA-Z0-9 ]{1,10}$"""

  val textTrimmed: Mapping[String] = text.transform[String](_.trim, identity)

  val textCleanedOfSpaces: String => String = _.replaceAll("\\s", "")

  val textCleanedOfSpacesAndDashes: String => String = textCleanedOfSpaces(_).replaceAll("-", "")

  val validAccountName: Mapping[String] = textTrimmed.verifying(accountNameConstraint)

  val validSortCode: Mapping[String] = text.transform[String](
    sortCode => textCleanedOfSpacesAndDashes(sortCode), identity
  ).verifying(sortCodeConstraint)

  val validAccountNumber: Mapping[String] = text.transform[String](
    accountNumber => zeroPadAccountNumber(textCleanedOfSpaces(accountNumber)), identity
  ).verifying(accountNumberConstraint)

  val validRollNumber: Mapping[String] = textTrimmed
    .transform[String](textCleanedOfSpaces(_), identity)
    .verifying(rollNumberConstraint)

  private def zeroPadAccountNumber(accountNumber: String): String = {
    if (accountNumber.length >= 6)
      StringUtils.leftPad(accountNumber, 8, "0")
    else
      accountNumber
  }

  val form: Form[BankAccount] =
    Form(
      mapping(
        "accountName" -> validAccountName,
        "sortCode" -> validSortCode,
        "accountNumber" -> validAccountNumber,
        "rollNumber" -> optional(validRollNumber)
      )(BankAccount.apply)(BankAccount.unapply)
    )

  def accountNameConstraint: Constraint[String] = Constraint[String]("constraint.account-name") { accountName =>
    if (accountName.isEmpty) Invalid(ValidationError("enter-bank-details.error.accountName.required"))
    else if (accountName.length > 60) Invalid(ValidationError("enter-bank-details.error.accountName.maxLength"))
    else if (!accountName.matches(validAccountNameRegex)) Invalid(ValidationError("enter-bank-details.error.accountName.format"))
    else Valid
  }

  def sortCodeConstraint: Constraint[String] = Constraint[String]("constraint.sort-code") { sc =>
    if (sc.length === 0) Invalid(ValidationError("enter-bank-details.error.sortCode.required"))
    else if (sc.length =!= 6) Invalid(ValidationError("enter-bank-details.error.sortCode.invalid"))
    else if (!sc.forall(_.isDigit)) Invalid(ValidationError("enter-bank-details.error.sortCode.invalid"))
    else if (!sc.matches(validSortCodeRegex)) Invalid(ValidationError("enter-bank-details.error.sortCode.invalid"))
    else Valid
  }

  def accountNumberConstraint: Constraint[String] = Constraint[String]("constraint.account-number") { accNum =>
    if (accNum.length === 0) Invalid(ValidationError("enter-bank-details.error.accountNumber.required"))
    else if (accNum.length =!= 8) Invalid(ValidationError("enter-bank-details.error.accountNumber.length"))
    else if (!accNum.forall(_.isDigit)) Invalid(ValidationError("enter-bank-details.error.accountNumber.length"))
    else Valid
  }

  def rollNumberConstraint: Constraint[String] = Constraint[String]("constraint.rollNumber") { rollNum =>
    if (rollNum.length > 10) Invalid(ValidationError("enter-bank-details.error.rollNumber.length"))
    else if (!rollNum.matches(validRollNumberRegex)) Invalid(ValidationError("enter-bank-details.error.rollNumber.format"))
    else Valid
  }

  private val sortCodeAndAccountNumberOverrides: Seq[FormError] = Seq(
    FormError("sortCode", ""), // 'turns off' the sortCode field error
    FormError("accountNumber", ""), // 'turns off' the accountNumber field error
    FormError("sortCodeAndAccountNumber", "enter-bank-details.bars.account.number.not.well.formatted")
  )
  val accountNumberNotWellFormatted: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "enter-bank-details.bars.account.number.not.well.formatted"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
  val sortCodeNotPresentOnEiscd: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "enter-bank-details.bars.sortcode.not.present.on.eiscd"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
  val sortCodeDoesNotSupportsDirectCredit: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError = FormError("sortCode", "enter-bank-details.bars.sortcode.does.not.support.direct.credit")
    )
  val nameDoesNotMatch: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError = FormError("accountName", "enter-bank-details.bars.account.name.no.match")
    )
  val nonStandardDetailsRequired: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError = FormError("rollNumber", "enter-bank-details.bars.account.roll.number.required")
    )
  val accountDoesNotExist: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "enter-bank-details.bars.account.does.not.exist"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
  val sortCodeOnDenyList: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "enter-bank-details.bars.sortcode.on.deny.list"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
  val otherBarsError: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "enter-bank-details.bars.other.error"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
}

final case class FormErrorWithFieldMessageOverrides(
    formError:             FormError,
    fieldMessageOverrides: Seq[FormError] = Seq.empty
)
