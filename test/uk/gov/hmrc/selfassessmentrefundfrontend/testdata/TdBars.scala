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

package uk.gov.hmrc.selfassessmentrefundfrontend.testdata

import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.response.{BarsAssessmentType, BarsErrorResponse, BarsValidateResponse, BarsVerifyResponse}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.BankAccountDetailsController.BankAccount

import java.time.Instant

object TdBars {

  val name              = "Account Name"
  val sortCode          = "123456"
  val bankAccountNumber = "12345678"
  val accountNumber     = bankAccountNumber

  val bankAccountInfo = BankAccount(
    accountName = name,
    sortCode = sortCode,
    accountNumber = bankAccountNumber,
    rollNumber = None
  )

  val barsValidateResponse = BarsValidateResponse(
    accountNumberIsWellFormatted = BarsAssessmentType.Yes,
    nonStandardAccountDetailsRequiredForBacs = BarsAssessmentType.No,
    sortCodeIsPresentOnEISCD = BarsAssessmentType.Yes,
    sortCodeSupportsDirectDebit = Some(BarsAssessmentType.Yes),
    sortCodeSupportsDirectCredit = None,
    sortCodeBankName = None,
    iban = None
  )

  val barsValidateResponseAccountNumNotWellFormatted = BarsValidateResponse(
    accountNumberIsWellFormatted = BarsAssessmentType.No,
    nonStandardAccountDetailsRequiredForBacs = BarsAssessmentType.No,
    sortCodeIsPresentOnEISCD = BarsAssessmentType.Yes,
    sortCodeSupportsDirectDebit = Some(BarsAssessmentType.Yes),
    sortCodeSupportsDirectCredit = None,
    sortCodeBankName = None,
    iban = None
  )

  val barsVerifyResponse = BarsVerifyResponse(
    accountNumberIsWellFormatted = BarsAssessmentType.Yes,
    accountExists = BarsAssessmentType.Yes,
    nameMatches = BarsAssessmentType.Yes,
    nonStandardAccountDetailsRequiredForBacs = BarsAssessmentType.No,
    sortCodeIsPresentOnEISCD = BarsAssessmentType.Yes,
    sortCodeSupportsDirectDebit = BarsAssessmentType.Yes,
    sortCodeSupportsDirectCredit = BarsAssessmentType.Yes,
    accountName = None,
    sortCodeBankName = None,
    iban = None
  )

  val barsVerifyResponseAcountDoesNotExist = BarsVerifyResponse(
    accountNumberIsWellFormatted = BarsAssessmentType.Yes,
    accountExists = BarsAssessmentType.No,
    nameMatches = BarsAssessmentType.Indeterminate,
    nonStandardAccountDetailsRequiredForBacs = BarsAssessmentType.Indeterminate,
    sortCodeIsPresentOnEISCD = BarsAssessmentType.Indeterminate,
    sortCodeSupportsDirectDebit = BarsAssessmentType.Indeterminate,
    sortCodeSupportsDirectCredit = BarsAssessmentType.Indeterminate,
    accountName = None,
    sortCodeBankName = None,
    iban = None
  )

  val barsVerifyResponseAllFields = BarsVerifyResponse(
    accountNumberIsWellFormatted = BarsAssessmentType.Yes,
    accountExists = BarsAssessmentType.Yes,
    nameMatches = BarsAssessmentType.Yes,
    nonStandardAccountDetailsRequiredForBacs = BarsAssessmentType.No,
    sortCodeIsPresentOnEISCD = BarsAssessmentType.Yes,
    sortCodeSupportsDirectDebit = BarsAssessmentType.Yes,
    sortCodeSupportsDirectCredit = BarsAssessmentType.Yes,
    accountName = Some("Account Name"),
    sortCodeBankName = Some("Lloyds"),
    iban = Some("GB59 HBUK 1234 5678")
  )

  val barsErrorResponse = BarsErrorResponse(
    code = "code",
    desc = "desc"
  )

  val futureDateTime: Instant = Instant.parse("2050-12-31T12:00:00.00Z")

}
