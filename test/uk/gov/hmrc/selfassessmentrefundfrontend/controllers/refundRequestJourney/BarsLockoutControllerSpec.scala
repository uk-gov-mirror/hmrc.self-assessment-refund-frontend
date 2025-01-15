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
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.BarsLockoutPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.{TdAll, TdBars}

class BarsLockoutControllerSpec extends ItSpec with BarsLockoutPageTesting {

  val controller: BarsLockoutController = fakeApplication().injector.instanceOf[BarsLockoutController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
    ()
  }

  override def fakeAuthConnector: Option[AuthConnector] = None

  "the Bars Lockout Controller" when {
    "called on 'barsLockout'" should {
      "show 'bars lockout' page" in {
        stubBarsVerifyStatus(lockedOut = true)
        stubBackendBusinessJourney(Some(TdAll.nino))
        val response = controller.barsLockout(TdAll.request)

        response.checkPageIsDisplayed(
          expectedHeading     = "You’ve tried to confirm your bank details too many times",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContent(TdBars.futureDateTime, "http://localhost:9081/report-quarterly/income-and-expenses/view"),
          expectedStatus      = Status.OK,
          withBackButton      = false,
          journey             = "request"
        )
      }
      "show 'bars lockout' page for Agent" in {
        AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
        stubBarsVerifyStatus(lockedOut = true)
        stubBackendBusinessJourney(Some(TdAll.nino))
        val response = controller.barsLockout(TdAll.request)

        response.checkPageIsDisplayed(
          expectedHeading     = "You’ve tried to confirm your bank details too many times",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks       = checkPageContent(TdBars.futureDateTime, "http://localhost:9081/report-quarterly/income-and-expenses/view/agents"),
          expectedStatus      = Status.OK,
          withBackButton      = false,
          journey             = "request"
        )
      }
      "show welsh 'bars lockout' page" in {
        stubBarsVerifyStatus(lockedOut = true)
        stubBackendBusinessJourney(Some(TdAll.nino))
        val response = controller.barsLockout(TdAll.welshRequest)

        response.checkPageIsDisplayed(
          expectedHeading     = "Rydych wedi ceisio cadarnhau’ch manylion banc ormod o weithiau",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContentWelsh(TdBars.futureDateTime, "http://localhost:9081/report-quarterly/income-and-expenses/view"),
          expectedStatus      = Status.OK,
          withBackButton      = false,
          journey             = "request",
          welsh               = true
        )
      }
    }

  }

}
