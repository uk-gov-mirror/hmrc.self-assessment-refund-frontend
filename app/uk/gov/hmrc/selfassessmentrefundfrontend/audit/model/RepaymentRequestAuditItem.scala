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

import play.api.libs.json.{JsValue, Json, OFormat, OWrites, Reads}

final case class RepaymentRequestAuditItem(
    etmpResult:                       String,
    userType:                         String,
    agentReferenceNumber:             Option[String],
    totalCreditAvailableForRepayment: String,
    unallocatedCredit:                String,
    amountChosen:                     String,
    barsResponse:                     Option[JsValue],
    reference:                        Option[String],
    nino:                             String,
    nrsSubmissionId:                  String,
    bankAccount:                      Option[BankAccountDetailsAudit]
)

object RepaymentRequestAuditItem {
  implicit def writes: OWrites[RepaymentRequestAuditItem] = Json.writes[RepaymentRequestAuditItem]

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[BankAccountDetailsAudit] = Json.format[BankAccountDetailsAudit]

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit def reads: Reads[RepaymentRequestAuditItem] = Json.reads[RepaymentRequestAuditItem]
}
