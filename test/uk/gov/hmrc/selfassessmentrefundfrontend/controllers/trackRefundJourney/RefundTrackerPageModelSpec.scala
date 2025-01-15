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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.selfassessmentrefundfrontend.TdRepayments
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod
import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.RefundTrackerPageModel.RefundTrackerModel
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService._

import java.time.LocalDate

class RefundTrackerPageModelSpec extends AnyWordSpec with Matchers with TdRepayments {

  "A RefundTrackerPageModel" when {
    "it is generated from a ProcessingTaxRepayment" should {
      val repayment = ProcessingTaxRepayment(
        Claim(no1, nino, 124.00, LocalDate.of(2021, 3, 3), Some(PaymentMethod.Card))
      )

      val model = RefundTrackerModel.processingConversion.mapFrom(repayment)
      "contain a formatted cash amount" in {
        model.amountClaimed shouldBe "£124"
      }
    }
    "it is generated from a ProcessingRiskingTaxRepayment" should {
      val repayment = ProcessingRiskingTaxRepayment(
        Claim(no1, nino, 124.00, LocalDate.of(2021, 3, 3), Some(PaymentMethod.Card))
      )

      val model = RefundTrackerModel.processingRiskingConversion.mapFrom(repayment)
      "contain a formatted cash amount" in {
        model.amountClaimed shouldBe "£124"
      }
    }
    "it is generated from an ApprovedTaxRepayment" should {
      val repayment = ApprovedTaxRepayment(
        Claim(no1, nino, 124.00, LocalDate.of(2021, 10, 9), Some(PaymentMethod.Card)),
        LocalDate.of(2021, 11, 10)
      )

      val model = RefundTrackerModel.approvedConversion.mapFrom(repayment)
      "contain a formatted cash amount" in {
        model.amountClaimed shouldBe "£124"
      }
    }
    "it is generated from a RejectedTaxRepayment" should {
      val repayment = RejectedTaxRepayment(
        Claim(no1, nino, 124.00, LocalDate.of(2021, 10, 9), Some(PaymentMethod.Card)),
        LocalDate.of(2021, 11, 10),
        Option("Issue with taxpayer bank")
      )

      val model = RefundTrackerModel.rejectedConversion.mapFrom(repayment)
      "contain a formatted cash amount" in {
        model.amountClaimed shouldBe "£124"
      }
    }
  }

}
