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

package uk.gov.hmrc.selfassessmentrefundfrontend.model

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.BankAccountDetailsController.BankAccount

final case class BankAccountInfo(
    name:          String,
    sortCode:      SortCode,
    accountNumber: AccountNumber,
    rollNumber:    Option[RollNumber] = None
)

object BankAccountInfo {
  def apply(bankAccount: BankAccount): BankAccountInfo =
    BankAccountInfo(
      name          = bankAccount.accountName,
      sortCode      = SortCode(bankAccount.sortCode),
      accountNumber = AccountNumber(bankAccount.accountNumber),
      rollNumber    = bankAccount.rollNumber.map(str => RollNumber(str))
    )

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[BankAccountInfo] = Json.format
}
