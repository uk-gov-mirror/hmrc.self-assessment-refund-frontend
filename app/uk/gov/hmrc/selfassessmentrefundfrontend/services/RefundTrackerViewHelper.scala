/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentrefundfrontend.services

import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.RefundTrackerPageModel.RefundTrackerModel
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService.TaxRepayment
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter
import java.time.LocalDate

class RefundTrackerViewHelper {

  def refundTrackerYearModelMap(taxRepayments: List[TaxRepayment]): Map[Int, Seq[RefundTrackerModel]] = {
    val repaymentsToRefundTrackerModel: Seq[RefundTrackerModel] = taxRepayments.map { repayment =>
      RefundTrackerModel(
        receivedOn    = repayment.claim.created,
        amountClaimed = AmountFormatter.formatAmount(repayment.claim.amount),
        status        = repayment.status,
        key           = repayment.claim.key
      )
    }
    val uniqueYears = repaymentsToRefundTrackerModel.collect(_.receivedOn.getYear).distinct
    uniqueYears.map (year => (
      year, repaymentsToRefundTrackerModel
      .filter(_.receivedOn.getYear == year)
      .sortBy(_.receivedOn)(Ordering[LocalDate].reverse)
    )).toMap
  }
}
