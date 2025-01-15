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

package uk.gov.hmrc.selfassessmentrefundfrontend.model.page

import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService.Claim
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter

final case class RefundRejectedPageModel(
    amount:      String,
    reference:   RequestNumber,
    tryAgainUrl: String
)

object RefundRejectedPageModel {
  def apply(claim: Claim, tryAgainUrl: String): RefundRejectedPageModel = {
    RefundRejectedPageModel(
      amount      = AmountFormatter.formatAmount(claim.amount),
      reference   = claim.key,
      tryAgainUrl = tryAgainUrl
    )
  }
}
