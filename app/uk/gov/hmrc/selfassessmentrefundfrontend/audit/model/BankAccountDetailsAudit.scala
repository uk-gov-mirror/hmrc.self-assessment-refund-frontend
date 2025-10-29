/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentrefundfrontend.audit.model

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.{AccountNumber, AccountType, BankAccountInfo, SortCode}

final case class BankAccountDetailsAudit(
  accountType:       String,
  accountHolderName: String,
  sortCode:          String,
  accountNumber:     String
)

object BankAccountDetailsAudit {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[BankAccountDetailsAudit] = Json.format[BankAccountDetailsAudit]

  def fromOptionalBankAccountInfo(
    optAccountType:     Option[AccountType],
    optBankAccountInfo: Option[BankAccountInfo]
  ): Option[BankAccountDetailsAudit] =
    if (optAccountType.isEmpty && optBankAccountInfo.isEmpty) {
      None
    } else {
      val accountType     = optAccountType.getOrElse(AccountType("Account type is missing"))
      val bankAccountInfo = optBankAccountInfo.getOrElse(
        BankAccountInfo(
          name = "Bank account info is missing",
          sortCode = SortCode("Bank account info is missing"),
          accountNumber = AccountNumber("Bank account info is missing")
        )
      )

      fromBankAccountInfo(accountType, bankAccountInfo)
    }

  def fromBankAccountInfo(
    accountType:     AccountType,
    bankAccountInfo: BankAccountInfo
  ): Option[BankAccountDetailsAudit] =
    Some(
      BankAccountDetailsAudit(
        accountType = accountType.name,
        accountHolderName = bankAccountInfo.name,
        sortCode = bankAccountInfo.sortCode.value,
        accountNumber = bankAccountInfo.accountNumber.value
      )
    )
}
