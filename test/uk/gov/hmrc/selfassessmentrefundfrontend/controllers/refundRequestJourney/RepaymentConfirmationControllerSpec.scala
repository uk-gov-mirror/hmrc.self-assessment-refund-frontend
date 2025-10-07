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

import java.time.OffsetDateTime
import play.api.http.Status
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import support.ItSpec
import support.stubbing.{AuthStub, CitizenDetailsStub}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.selfassessmentrefundfrontend.TdRepayments
import uk.gov.hmrc.selfassessmentrefundfrontend.model.Amount
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod.Card
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.RepaymentConfirmationPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport._

class RepaymentConfirmationControllerSpec extends ItSpec with TdRepayments with RepaymentConfirmationPageTesting {

  private val controller = app.injector.instanceOf[RepaymentConfirmationController]

  override def fakeAuthConnector: Option[AuthConnector] = None

  "the RepaymentConfirmationController for an individual" when {
    "there is card on file" should {
      "display correct content and link to the tracker page" in {
        val amount = Amount(Some(123), None, None, Some(123), Some(123))
        val timeOfConfirmation = OffsetDateTime.parse("2023-12-01T17:35:30+01:00")
        val fakeRequest = FakeRequest("GET", "/refund-request-received/1234567890")
          .withAuthToken()
          .withRequestId()
          .withSessionId()

        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
        stubBackendBusinessJourney(method = Some(Card))
        stubBackendJourney()
        stubPOSTBackendAudit()

        val call = controller.confirmation(RequestNumber("1234567890"))
        val result = call()(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading     = "Refund request received",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContent(amount.totalCreditAvailableForRepayment.getOrElse(BigDecimal(0.0)), timeOfConfirmation, "1234567890", "12345678", isCardOnFile = true, isAgent = false),
          expectedStatus      = Status.OK,
          withBackButton      = false,
          journey             = "request"
        )
      }
      "display correct content in welsh and link to the tracker page" in {
        val amount = Amount(Some(123), None, None, Some(123), Some(123))
        val timeOfConfirmation = OffsetDateTime.parse("2023-12-01T17:35:30+01:00")
        val fakeRequest = FakeRequest("GET", "/refund-request-received/1234567890")
          .withAuthToken()
          .withRequestId()
          .withSessionId()
          .withCookies(Cookie("PLAY_LANG", "cy"))

        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
        stubBackendBusinessJourney(method = Some(Card))
        stubBackendJourney()
        stubPOSTBackendAudit()

        val call = controller.confirmation(RequestNumber("1234567890"))
        val result = call()(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading     = "Cais am ad-daliad wedi dod i law",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContentWelsh(amount.totalCreditAvailableForRepayment.getOrElse(BigDecimal(0.0)), timeOfConfirmation, "1234567890", "12345678", isCardOnFile = true, isAgent = false),
          expectedStatus      = Status.OK,
          withBackButton      = false,
          journey             = "request",
          welsh               = true
        )
      }
    }

    "there is no card on file" should {
      "display correct content and default link to the tracker page" in {
        val amount = Amount(Some(123), None, None, Some(123), Some(123))
        val timeOfConfirmation = OffsetDateTime.parse("2023-12-01T17:35:30+01:00")
        val fakeRequest = FakeRequest("GET", "/refund-request-received/1234567890")
          .withAuthToken()
          .withRequestId()
          .withSessionId()

        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
        stubBackendBusinessJourney()
        stubBackendJourney()
        stubPOSTBackendAudit()

        val call = controller.confirmation(RequestNumber("1234567890"))
        val result = call()(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading     = "Refund request received",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageContent(amount.totalCreditAvailableForRepayment.getOrElse(BigDecimal(0.0)), timeOfConfirmation, "1234567890", "12345678", isCardOnFile = false, isAgent = false),
          expectedStatus      = Status.OK,
          withBackButton      = false,
          journey             = "request"
        )
      }
    }
  }

  "the RepaymentConfirmationController for an agent" when {
    "there is card on file" should {
      "display correct content and link to the tracker page" in {
        val amount = Amount(Some(123), None, None, Some(123), Some(123))
        val timeOfConfirmation = OffsetDateTime.parse("2023-12-01T17:35:30+01:00")
        val fakeRequest = FakeRequest("GET", "/refund-request-received/1234567890")
          .withAuthToken()
          .withRequestId()
          .withSessionId()

        stubBarsVerifyStatus()
        AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
        stubBackendBusinessJourney(method = Some(Card), nino = Some(Nino("AB123456C")))
        stubBackendJourney()
        stubPOSTBackendAudit()
        CitizenDetailsStub.getCitizenDetails200("AB123456C", "0987654321")

        val call = controller.confirmation(RequestNumber("1234567890"))
        val result = call()(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading     = "Refund request received",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks       = checkPageContent(amount.totalCreditAvailableForRepayment.getOrElse(BigDecimal(0.0)), timeOfConfirmation, "1234567890", "12345678", isCardOnFile = true, isAgent = true, isClientUtrPresent = true),
          expectedStatus      = Status.OK,
          withBackButton      = false,
          journey             = "request"
        )
      }
    }
    "there is no card on file" should {
      "display correct content and default link to the tracker page" in {
        val amount = Amount(Some(123), None, None, Some(123), Some(123))
        val timeOfConfirmation = OffsetDateTime.parse("2023-12-01T17:35:30+01:00")
        val fakeRequest = FakeRequest("GET", "/refund-request-received/1234567890")
          .withAuthToken()
          .withRequestId()
          .withSessionId()

        stubBarsVerifyStatus()
        AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
        stubBackendBusinessJourney(nino = Some(Nino("AB123456C")))
        stubBackendJourney()
        stubPOSTBackendAudit()
        CitizenDetailsStub.getCitizenDetailsUpstreamError("AB123456C", 404)

        val call = controller.confirmation(RequestNumber("1234567890"))
        val result = call()(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading     = "Refund request received",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks       = checkPageContent(amount.totalCreditAvailableForRepayment.getOrElse(BigDecimal(0.0)), timeOfConfirmation, "1234567890", "12345678", isCardOnFile = false, isAgent = true),
          expectedStatus      = Status.OK,
          withBackButton      = false,
          journey             = "request"
        )
      }
    }
  }

}
