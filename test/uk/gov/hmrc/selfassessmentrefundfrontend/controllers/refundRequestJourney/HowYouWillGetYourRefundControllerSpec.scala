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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.test.Helpers._
import support.ItSpec
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod.{BACS, Card}
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.HowYouWillGetYourRefundPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll

class HowYouWillGetYourRefundControllerSpec extends ItSpec with HowYouWillGetYourRefundPageTesting {

  val controller: HowYouWillGetYourRefundController = fakeApplication().injector.instanceOf[HowYouWillGetYourRefundController]

  "the How You Will Get Your Refund Controller" when {
    "called on 'onPageLoad" should {
      "show 'How You Will Get Your Refund' page" in {
        setupStubsForShowRepayment(lastPaymentByCard = true)
        val response = controller.onPageLoad(TdAll.request)
        response.checkPageIsDisplayed(
          expectedHeading     = "How you will get the refund",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContent(isAgent = false),
          expectedStatus      = Status.OK,
          journey             = "request"
        )
      }
      "show welsh 'How You Will Get Your Refund' page" in {
        setupStubsForShowRepayment(lastPaymentByCard = true)
        val response = controller.onPageLoad(TdAll.welshRequest)
        response.checkPageIsDisplayed(
          expectedHeading     = "Sut y byddwch yn cael yr ad-daliad",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContentWelsh(isAgent = false),
          expectedStatus      = Status.OK,
          journey             = "request",
          welsh               = true
        )
      }
    }
    "called on 'onPageLoadAgent" should {
      "show 'How Your Client Will Get The Refund' page" in {
        setupStubsForShowRepayment(lastPaymentByCard = true)
        val response = controller.onPageLoadAgent(TdAll.request)
        response.checkPageIsDisplayed(
          expectedHeading     = "How your client will get the refund",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks       = checkPageContent(isAgent = true),
          expectedStatus      = Status.OK,
          journey             = "request"
        )
      }
      "show welsh 'How Your Client Will Get The Refund' page" in {
        setupStubsForShowRepayment(lastPaymentByCard = true)
        val response = controller.onPageLoadAgent(TdAll.welshRequest)
        response.checkPageIsDisplayed(
          expectedHeading     = "Sut y bydd eich cleient yn cael yr ad-daliad",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks       = checkPageContentWelsh(isAgent = true),
          expectedStatus      = Status.OK,
          journey             = "request",
          welsh               = true
        )
      }
    }
    "called on 'onSubmit'" should {
      "redirect to /type-of-bank-account" in {
        setupStubsForShowRepayment(lastPaymentByCard = true)
        val response = controller.onSubmit(TdAll.request)
        status(response) shouldBe Status.SEE_OTHER
        redirectLocation(response) shouldBe Some("/request-a-self-assessment-refund/type-of-bank-account")
      }
    }
  }

  def setupStubsForShowRepayment(lastPaymentByCard: Boolean): StubMapping = {
    lastPaymentByCard match {
      case true =>
        stubBackendLastPaymentMethod(Card)
        stubBackendBusinessJourney(method = Some(Card))
      case false =>
        stubBackendLastPaymentMethod(BACS)
        stubBackendBusinessJourney()
    }
  }

}
