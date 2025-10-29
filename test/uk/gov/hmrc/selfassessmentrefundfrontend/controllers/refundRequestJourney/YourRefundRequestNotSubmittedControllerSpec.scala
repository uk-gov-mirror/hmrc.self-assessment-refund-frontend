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

import play.api.http.Status
import support.ItSpec
import support.stubbing.AuthStub
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.YourRefundRequestNotSubmittedPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll

class YourRefundRequestNotSubmittedControllerSpec extends ItSpec with YourRefundRequestNotSubmittedPageTesting {
  override def fakeAuthConnector: Option[AuthConnector]   = None
  val controller: YourRefundRequestNotSubmittedController =
    fakeApplication().injector.instanceOf[YourRefundRequestNotSubmittedController]

  "YourRefundRequestNotSubmittedController" when {
    "called on 'show'" should {
      "show 'Your Refund Request Not Submitted' page with button link for Individuals" in {
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
        stubBackendBusinessJourney()
        val result = controller.show(TdAll.request)

        result.checkPageIsDisplayed(
          expectedHeading = "Your refund request has not been submitted",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks = checkPageContent(isAgent = false),
          expectedStatus = Status.OK,
          withBackButton = false,
          journey = "request"
        )
      }
      "show welsh 'Your Refund Request Not Submitted' page with button link for Individuals" in {
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
        stubBackendBusinessJourney()
        val result = controller.show(TdAll.welshRequest)

        result.checkPageIsDisplayed(
          expectedHeading = "Nid yw’ch cais am ad-daliad wedi’i gyflwyno",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks = checkPageContentWelsh(isAgent = false),
          expectedStatus = Status.OK,
          withBackButton = false,
          journey = "request",
          welsh = true
        )
      }

      "show 'Your Refund Request Not Submitted' page with button link for Organisations" in {
        AuthStub.authorise(AffinityGroup.Organisation, ConfidenceLevel.L250)
        stubBackendBusinessJourney()
        val result = controller.show(TdAll.request)

        result.checkPageIsDisplayed(
          expectedHeading = "Your refund request has not been submitted",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks = checkPageContent(isAgent = false),
          expectedStatus = Status.OK,
          withBackButton = false,
          journey = "request"
        )
      }

      "show 'Your Refund Request Not Submitted' page with button link for Agents" in {
        AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
        stubBackendBusinessJourney()
        val result = controller.show(TdAll.request)

        result.checkPageIsDisplayed(
          expectedHeading = "Your refund request has not been submitted",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks = checkPageContent(isAgent = true),
          expectedStatus = Status.OK,
          withBackButton = false,
          journey = "request"
        )
      }
    }
  }

}
