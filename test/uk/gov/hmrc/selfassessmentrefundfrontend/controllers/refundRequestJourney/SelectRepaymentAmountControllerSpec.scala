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
import support.stubbing.AuthStub
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.mvc.{Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.{ItSpec, WireMockSupport}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.model.Amount
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod.{BACS, Card}
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.SelectAmountPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

import scala.concurrent.Future

class SelectRepaymentAmountControllerSpec
  extends ItSpec
  with GuiceOneAppPerSuite
  with WireMockSupport
  with SelectAmountPageTesting {
  that: TestSuite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
    ()
  }

  override def fakeAuthConnector: Option[AuthConnector] = None

  private val amountController = app.injector.instanceOf[SelectRepaymentAmountController]

  private val selectAmountPageHeading = "How much do you want to be refunded?"
  private val selectAmountPageHeadingWelsh = "Faint o ad-daliad yr hoffech ei gael?"

  "GET /refund-amount" when {
    val fakeRequest = FakeRequest("GET", "/self-assessment-refund-frontend/refund-amount")
      .withSessionId().withAuthToken()

    "a journey is found" when {
      "an amount is found via the backend" should {
        "display 'select amount' page with default back link url" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney()

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = selectAmountPageHeading,
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks       = checkPageContent(testAmount),
            expectedStatus      = Status.OK,
            journey             = "request"
          )
        }

        "display welsh 'select amount' page with default back link url" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney()

          val result: Future[Result] = amountController.selectAmount(fakeRequest.withCookies(Cookie("PLAY_LANG", "cy")))

          result.checkPageIsDisplayed(
            expectedHeading     = selectAmountPageHeadingWelsh,
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks       = checkPageContentWelsh(testAmount),
            expectedStatus      = Status.OK,
            journey             = "request",
            welsh               = true
          )
        }

        "display 'select amount' page without suggested amount option if it is less than 0" in {
          val amount = Amount(Some(BigDecimal(45.67)), None, None, availableCredit = Some(BigDecimal(45.67)), balanceDueWithin30Days = Some(BigDecimal(122.34)))

          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(amount))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = selectAmountPageHeading,
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks       = checkPageContent(amount, withoutSuggestedAmount = true),
            expectedStatus      = Status.OK,
            journey             = "request"
          )
        }

        "display 'select amount' page without suggested amount option if it is 0" in {
          val amount = Amount(Some(BigDecimal(45.67)), None, None, availableCredit = Some(BigDecimal(45.67)), balanceDueWithin30Days = Some(BigDecimal(45.67)))

          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(amount))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = selectAmountPageHeading,
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks       = checkPageContent(amount, withoutSuggestedAmount = true),
            expectedStatus      = Status.OK,
            journey             = "request"
          )
        }

        "display 'there is a problem' generic page if amount from V&C is not matching the amount from API#1553" in {
          val amount = Amount(Some(BigDecimal(123)), None, None, availableCredit = Some(BigDecimal(122)), balanceDueWithin30Days = Some(BigDecimal(45)))

          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(amount))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading          = "Sorry, there is a problem with the service",
            expectedServiceLink      = "",
            expectedStatus           = Status.INTERNAL_SERVER_ERROR,
            withBackButton           = false,
            expectedTitleIfDifferent = Some("Sorry, there is a problem with the service - 500")
          )
        }

        "display 'there is a problem' generic page if amount is missing" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = None)

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading          = "Sorry, there is a problem with the service",
            expectedServiceLink      = "",
            expectedStatus           = Status.INTERNAL_SERVER_ERROR,
            withBackButton           = false,
            expectedTitleIfDifferent = Some("Sorry, there is a problem with the service - 500")
          )
        }

        "display 'there is a problem' generic page if availableCredit in amount is missing" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(testAmount.copy(availableCredit = None)))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading          = "Sorry, there is a problem with the service",
            expectedServiceLink      = "",
            expectedStatus           = Status.INTERNAL_SERVER_ERROR,
            withBackButton           = false,
            expectedTitleIfDifferent = Some("Sorry, there is a problem with the service - 500")
          )
        }

        "display 'there is a problem' generic page if balanceDueWithin30Days in amount is missing" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(testAmount.copy(balanceDueWithin30Days = None)))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading          = "Sorry, there is a problem with the service",
            expectedServiceLink      = "",
            expectedStatus           = Status.INTERNAL_SERVER_ERROR,
            withBackButton           = false,
            expectedTitleIfDifferent = Some("Sorry, there is a problem with the service - 500")
          )
        }
      }
    }
  }

  "POST /refund-amount" when {
    val fakeRequestBase = FakeRequest("POST", "/self-assessment-refund-frontend/refund-amount")
      .withSessionId().withAuthToken()

    "form has errors (no amount selected)" should {
      stubBackendPersonalJourney()
      val fakeRequestWithFormWithError = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123")

      "return 200, and html with the amount" in {
        stubGETJourneyWithBankDetails()
        stubBarsVerifyStatus()

        val result: Future[Result] = amountController.submitAmount(fakeRequestWithFormWithError)

        result.checkPageIsDisplayed(
          expectedHeading     = selectAmountPageHeading,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageWithFormError(testAmount, "Select how much you want to be refunded", "#choice-suggested"),
          expectedStatus      = Status.OK,
          withError           = true,
          journey             = "request"
        )
      }

      "return 200, and welsh html with the amount" in {
        stubGETJourneyWithBankDetails()
        stubBarsVerifyStatus()

        val result: Future[Result] = amountController.submitAmount(fakeRequestWithFormWithError.withCookies(Cookie("PLAY_LANG", "cy")))

        result.checkPageIsDisplayed(
          expectedHeading     = selectAmountPageHeadingWelsh,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageWithFormErrorWelsh(testAmount, "Dewiswch faint o ad-daliad yr hoffech ei gael", "#choice-suggested"),
          expectedStatus      = Status.OK,
          withError           = true,
          journey             = "request",
          welsh               = true
        )
      }
    }

    "form with no suggested amount has errors (no amount selected)" should {
      stubBackendPersonalJourney()
      val fakeRequestWithFormWithError = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123")

      "return 200, and html with the amount" in {
        val amount = Amount(Some(BigDecimal(123)), None, None, availableCredit = Some(BigDecimal(123)), balanceDueWithin30Days = Some(BigDecimal(123)))

        stubGETJourneyWithBankDetails("", amount)
        stubBarsVerifyStatus()

        val result: Future[Result] = amountController.submitAmount(fakeRequestWithFormWithError)

        result.checkPageIsDisplayed(
          expectedHeading     = selectAmountPageHeading,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageWithFormError(amount, "Select how much you want to be refunded", "#choice-full", withoutSuggestedAmount = true),
          expectedStatus      = Status.OK,
          withError           = true,
          journey             = "request"
        )
      }

      "return 200, and welsh html with the amount" in {
        val amount = Amount(Some(BigDecimal(45.67)), None, None, availableCredit = Some(BigDecimal(45.67)), balanceDueWithin30Days = Some(BigDecimal(123)))

        stubGETJourneyWithBankDetails("", amount)
        stubBarsVerifyStatus()

        val result: Future[Result] = amountController.submitAmount(fakeRequestWithFormWithError.withCookies(Cookie("PLAY_LANG", "cy")))

        result.checkPageIsDisplayed(
          expectedHeading     = selectAmountPageHeadingWelsh,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageWithFormErrorWelsh(amount, "Dewiswch faint o ad-daliad yr hoffech ei gael", "#choice-full", withoutSuggestedAmount = true),
          expectedStatus      = Status.OK,
          withError           = true,
          journey             = "request",
          welsh               = true
        )
      }
    }

    "form has errors (partial selected, amount not provided)" should {
      stubBackendPersonalJourney()
      val fakeRequestWithFormWithError = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "partial")

      "return 200, and html with the amount" in {
        stubGETJourneyWithBankDetails()
        stubBarsVerifyStatus()

        val result: Future[Result] = amountController.submitAmount(fakeRequestWithFormWithError)

        result.checkPageIsDisplayed(
          expectedHeading     = selectAmountPageHeading,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageWithFormError(testAmount, "Enter a refund amount", "#different-amount"),
          expectedStatus      = Status.OK,
          withError           = true,
          journey             = "request"
        )
      }

      "return 200, and welsh html with the amount" in {
        stubGETJourneyWithBankDetails()
        stubBarsVerifyStatus()

        val result: Future[Result] = amountController.submitAmount(fakeRequestWithFormWithError.withCookies(Cookie("PLAY_LANG", "cy")))

        result.checkPageIsDisplayed(
          expectedHeading     = selectAmountPageHeadingWelsh,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageWithFormErrorWelsh(testAmount, "Nodwch swm ar gyfer yr ad-daliad", "#different-amount"),
          expectedStatus      = Status.OK,
          withError           = true,
          journey             = "request",
          welsh               = true
        )
      }
    }

    "the full amount has been selected" when {
      val fakeRequestWithValidFormFullAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "full")

      "affinity is Individual" when {
        "users last payment was by card" should {
          "set the amount via the backend and redirect to /how-you-will-get-the-refund" in {
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(Card)

            val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/how-you-will-get-the-refund")
          }
        }

        "users last payment was by BACS or P0" should {
          "set the amount via the backend and redirect to /we-need-your-bank-details" in {
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(BACS)

            val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/we-need-your-bank-details")
          }
        }
      }

      "affinity is Agent" when {
        "users last payment was by card" should {
          "set the amount via the backend and redirect to /how-your-client-will-get-the-refund" in {
            AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(Card)

            val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/how-your-client-will-get-the-refund")
          }
        }

        "users last payment was by BACS or P0" should {
          "set the amount via the backend and redirect to /we-need-your-clients-bank-details" in {
            AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(BACS)

            val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/we-need-your-clients-bank-details")
          }
        }
      }
    }

    "a partial amount has been selected" when {
      val fakeRequestWithValidFormPartialAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "partial", "amount" -> "80")

      "affinity is Individual" when {
        "users last payment was by card" should {
          "set the amount via the backend and redirect to /how-you-will-get-the-refund" in {
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(Card)

            val result = amountController.submitAmount(fakeRequestWithValidFormPartialAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/how-you-will-get-the-refund")
          }
        }

        "users last payment was by BACS or P0" should {
          "set the amount via the backend and redirect to /we-need-your-bank-details" in {
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(BACS)

            val result = amountController.submitAmount(fakeRequestWithValidFormPartialAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/we-need-your-bank-details")
          }
        }
      }

      "affinity is Agent" when {
        "users last payment was by card" should {
          "set the amount via the backend and redirect to /how-your-client-will-get-the-refund" in {
            AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(Card)

            val result = amountController.submitAmount(fakeRequestWithValidFormPartialAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/how-your-client-will-get-the-refund")
          }
        }

        "users last payment was by BACS or P0" should {
          "set the amount via the backend and redirect to /we-need-your-clients-bank-details" in {
            AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(BACS)

            val result = amountController.submitAmount(fakeRequestWithValidFormPartialAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/we-need-your-clients-bank-details")
          }
        }
      }
    }

    "a suggested amount has been selected" when {
      val fakeRequestWithValidFormSuggestedAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "suggested", "amount" -> "80")

      "affinity is Individual" when {
        "users last payment was by card" should {
          "set the amount via the backend and redirect to /how-you-will-get-the-refund" in {
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(Card)

            val result = amountController.submitAmount(fakeRequestWithValidFormSuggestedAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/how-you-will-get-the-refund")
          }
        }

        "users last payment was by BACS or P0" should {
          "set the amount via the backend and redirect to /we-need-your-bank-details" in {
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(BACS)

            val result = amountController.submitAmount(fakeRequestWithValidFormSuggestedAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/we-need-your-bank-details")
          }
        }
      }

      "affinity is Agent" when {
        "users last payment was by card" should {
          "set the amount via the backend and redirect to /how-your-client-will-get-the-refund" in {
            AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(Card)

            val result = amountController.submitAmount(fakeRequestWithValidFormSuggestedAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/how-your-client-will-get-the-refund")
          }
        }

        "users last payment was by BACS or P0" should {
          "set the amount via the backend and redirect to /we-need-your-clients-bank-details" in {
            AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
            stubBackendJourneyId()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            stubBackendLastPaymentMethod(BACS)

            val result = amountController.submitAmount(fakeRequestWithValidFormSuggestedAmount)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/we-need-your-clients-bank-details")
          }
        }
      }
    }

    "display 'there is a problem' generic page if amount is missing" in {
      val fakeRequestWithValidFormFullAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "full")

      stubBackendJourneyId()
      stubPOSTJourney()
      stubBackendBusinessJourney(amount = None)

      val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "display 'there is a problem' generic page if availableCredit in amount is missing" in {
      val fakeRequestWithValidFormFullAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "full")

      stubBackendJourneyId()
      stubPOSTJourney()
      stubBackendBusinessJourney(amount = Some(testAmount.copy(availableCredit = None)))

      val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "display 'there is a problem' generic page if balanceDueWithin30Days in amount is missing" in {
      val fakeRequestWithValidFormFullAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "full")

      stubBackendJourneyId()
      stubPOSTJourney()
      stubBackendBusinessJourney(amount = Some(testAmount.copy(balanceDueWithin30Days = None)))

      val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }
}
