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

package uk.gov.hmrc.selfassessmentrefundfrontend.model

import cats.implicits.catsSyntaxEq
import play.api.libs.json.{Json, OFormat}

final case class BankDetails(
  accountType:   String,
  name:          String,
  sortCode:      String,
  accountNumber: String,
  rollNumber:    Option[String]
) {
  def matches(accountType: AccountType, bankAccountInfo: BankAccountInfo): Boolean =
    this.accountType === accountType.name &&
      this.name === bankAccountInfo.name &&
      this.sortCode === bankAccountInfo.sortCode.value &&
      this.accountNumber === bankAccountInfo.accountNumber.value &&
      this.rollNumber === bankAccountInfo.rollNumber.map(_.toString)
}

object BankDetails {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[BankDetails] = Json.format[BankDetails]

}
