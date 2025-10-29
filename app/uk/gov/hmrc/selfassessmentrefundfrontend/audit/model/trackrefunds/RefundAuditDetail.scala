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

package uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.trackrefunds

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService.TaxRepayment
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService._

import java.time.LocalDate
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod

final case class RefundAuditDetail(
  refundReference:     RequestNumber,
  amount:              BigDecimal,
  status:              String,
  dateRefundRequested: LocalDate,
  repaymentDate:       Option[LocalDate],
  repaymentMethod:     Option[PaymentMethod]
)

object RefundAuditDetail {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[RefundAuditDetail] = Json.format[RefundAuditDetail]

  def fromTaxRepayment(
    taxRepayments: List[TaxRepayment]
  ): List[RefundAuditDetail] =
    taxRepayments.map { taxRepayment =>
      RefundAuditDetail(
        refundReference = taxRepayment.claim.key,
        amount = taxRepayment.claim.amount,
        status = taxRepayment match {
          case ProcessingTaxRepayment(_)        =>
            "Processing"
          case ProcessingRiskingTaxRepayment(_) =>
            "ProcessingRisking"
          case ApprovedTaxRepayment(_, _)       =>
            "Approved"
          case RejectedTaxRepayment(_, _, _)    =>
            "Rejected"
        },
        dateRefundRequested = taxRepayment.claim.created,
        repaymentDate = taxRepayment match {
          case ProcessingRiskingTaxRepayment(_) | ProcessingTaxRepayment(_) =>
            None
          case ApprovedTaxRepayment(_, completed)                           =>
            Some(completed)
          case RejectedTaxRepayment(_, completed, _)                        =>
            Some(completed)
        },
        repaymentMethod = taxRepayment.claim.repaymentMethod
      )
    }
}
