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

import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.mvc.{Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.stubbing.AuthStub
import support.{ItSpec, WireMockSupport}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.WeNeedBankDetailsPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

import scala.concurrent.Future

class WeNeedBankDetailsControllerSpec
  extends ItSpec
  with GuiceOneAppPerSuite
  with WireMockSupport
  with WeNeedBankDetailsPageTesting {
  that: TestSuite =>

  private val bankDetailsController = app.injector.instanceOf[WeNeedBankDetailsController]

  private val bankDetailsPageHeading = "We need your bank details"
  private val bankDetailsPageHeadingWelsh = "Mae angen eich manylion banc arnom"
  private val bankDetailsPageHeadingAgent = "We need your client’s bank details"
  private val bankDetailsPageHeadingAgentWelsh = "Mae angen manylion banc eich cleient arnom"

  "GET /we-need-your-bank-details" when {
    val fakeRequest = FakeRequest("GET", "/request-a-self-assessment-refund/we-need-your-bank-details")
      .withSessionId().withAuthToken()

    "called" should {
      "display 'we need your bank details' page with correct content" in {
        stubBackendJourneyId()
        stubBackendPersonalJourney()

        val result: Future[Result] = bankDetailsController.onPageLoad(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading     = bankDetailsPageHeading,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContent(isAgent = false),
          expectedStatus      = Status.OK,
          journey             = "request"
        )
      }
      "display welsh 'we need your bank details' page with correct content" in {
        stubBackendJourneyId()
        stubBackendPersonalJourney()

        val result: Future[Result] = bankDetailsController.onPageLoad(fakeRequest.withCookies(Cookie("PLAY_LANG", "cy")))

        result.checkPageIsDisplayed(
          expectedHeading     = bankDetailsPageHeadingWelsh,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContentWelsh(isAgent = false),
          expectedStatus      = Status.OK,
          journey             = "request",
          welsh               = true
        )
      }
    }
  }

  "GET /we-need-your-clients-bank-details" when {
    val fakeRequest = FakeRequest("GET", "/request-a-self-assessment-refund/we-need-your-clients-bank-details")
      .withSessionId().withAuthToken()

    "called" should {
      "display 'we need your client's bank details' page with correct content" in {
        stubBackendJourneyId()
        stubBackendPersonalJourney()

        val result: Future[Result] = bankDetailsController.onPageLoadAgent(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading     = bankDetailsPageHeadingAgent,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks       = checkPageContent(isAgent = true),
          expectedStatus      = Status.OK,
          journey             = "request"
        )
      }
      "display welsh 'we need your client's bank details' page with correct content" in {
        stubBackendJourneyId()
        stubBackendPersonalJourney()

        val result: Future[Result] = bankDetailsController.onPageLoadAgent(fakeRequest.withCookies(Cookie("PLAY_LANG", "cy")))

        result.checkPageIsDisplayed(
          expectedHeading     = bankDetailsPageHeadingAgentWelsh,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks       = checkPageContentWelsh(isAgent = true),
          expectedStatus      = Status.OK,
          journey             = "request",
          welsh               = true
        )
      }
    }
  }

  "POST /we-need-your-bank-details" when {
    val fakeRequest = FakeRequest("POST", "/request-a-self-assessment-refund/we-need-your-bank-details")
      .withSessionId().withAuthToken()

    "called" should {
      "redirect to /request-a-self-assessment-refund/type-of-bank-account" in {
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
        stubBackendPersonalJourney()

        val result: Future[Result] = bankDetailsController.onSubmit(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/type-of-bank-account")
      }
    }
  }
}
