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
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.AuditFlags
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod.{BACS, Card}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{Journey, JourneyTypes}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.SelectAmountPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

import java.time.OffsetDateTime
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
    val fakeRequest = FakeRequest("GET", "/request-a-self-assessment-refund/refund-amount")
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

        "display 'select amount' page without suggested amount option if unallocatedCredit is less than 0" in {
          val amount = Amount(Some(BigDecimal(45.67)), None, None, totalCreditAvailableForRepayment = Some(BigDecimal(45.67)), unallocatedCredit = Some(BigDecimal(-122.34)))

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

        "display 'select amount' page without suggested amount option if unallocatedCredit is 0" in {
          val amount = Amount(Some(BigDecimal(45.67)), None, None, totalCreditAvailableForRepayment = Some(BigDecimal(45.67)), unallocatedCredit = Some(BigDecimal(0.0)))

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

        "display 'select amount' page without suggested amount option if unallocatedCredit is greater than total available for repayment" in {
          val amount = Amount(Some(BigDecimal(45.67)), None, None, totalCreditAvailableForRepayment = Some(BigDecimal(45.67)), unallocatedCredit = Some(BigDecimal(123.45)))

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

        "display 'select amount' page without suggested amount option if unallocatedCredit is equal total available for repayment" in {
          val amount = Amount(Some(BigDecimal(45.67)), None, None, totalCreditAvailableForRepayment = Some(BigDecimal(45.67)), unallocatedCredit = Some(BigDecimal(45.67)))

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

        "display 'there is a problem' page if amount from V&C is not matching the amount from HIP-API#5277" in {
          val amount = Amount(Some(BigDecimal(123)), None, None, totalCreditAvailableForRepayment = Some(BigDecimal(122)), unallocatedCredit = Some(BigDecimal(45)))

          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(amount))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = "Sorry, there is a problem with the service",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            journey             = "request",
            expectedStatus      = Status.INTERNAL_SERVER_ERROR,
            withBackButton      = false
          )
        }

        "display 'there is a problem' page if amount is missing" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = None)

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = "Sorry, there is a problem with the service",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            journey             = "request",
            expectedStatus      = Status.INTERNAL_SERVER_ERROR,
            withBackButton      = false
          )
        }

        "display 'there is a problem' page if totalCreditAvailableForRepayment in amount is missing" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(testAmount.copy(totalCreditAvailableForRepayment = None)))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = "Sorry, there is a problem with the service",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            journey             = "request",
            expectedStatus      = Status.INTERNAL_SERVER_ERROR,
            withBackButton      = false
          )
        }

        "display 'there is a problem' page if unallocatedCredit in amount is missing" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(testAmount.copy(unallocatedCredit = None)))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = "Sorry, there is a problem with the service",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            journey             = "request",
            expectedStatus      = Status.INTERNAL_SERVER_ERROR,
            withBackButton      = false
          )
        }

        "display 'there is a problem' page if totalCreditAvailableForRepayment in amount is 0" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(testAmount.copy(totalCreditAvailableForRepayment = Some(0.0))))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = "Sorry, there is a problem with the service",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            journey             = "request",
            expectedStatus      = Status.INTERNAL_SERVER_ERROR,
            withBackButton      = false
          )
        }

        "display 'there is a problem' page if totalCreditAvailableForRepayment in amount is less than 0" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(backReturnUrl = true, amount = Some(testAmount.copy(totalCreditAvailableForRepayment = Some(-123.45))))

          val result: Future[Result] = amountController.selectAmount(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = "Sorry, there is a problem with the service",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            journey             = "request",
            expectedStatus      = Status.INTERNAL_SERVER_ERROR,
            withBackButton      = false
          )
        }
      }
    }
  }

  "POST /refund-amount" when {
    val fakeRequestBase = FakeRequest("POST", "/request-a-self-assessment-refund/refund-amount")
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
        val amount = Amount(Some(BigDecimal(123)), None, None, totalCreditAvailableForRepayment = Some(BigDecimal(123)), unallocatedCredit = Some(BigDecimal(123)))

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
        val amount = Amount(Some(BigDecimal(123)), None, None, totalCreditAvailableForRepayment = Some(BigDecimal(123)), unallocatedCredit = Some(BigDecimal(123)))

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

    for (amountSamples <- Seq("80", " 80 ", "80.00", "8 0", " 8,0 ", "8,0.0", "£80", "£ 80.0")) //amounts with allowed characters: spaces, commas, full stops, pound sign
      s"a partial amount has been selected [$amountSamples]" when {
        val fakeRequestWithValidFormPartialAmount = fakeRequestBase
          .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "partial", "amount" -> amountSamples)
        val testAmount: Amount = Amount(Some(BigDecimal(123)), Some(BigDecimal(80)), Some(true), totalCreditAvailableForRepayment = Some(BigDecimal(123)), unallocatedCredit = Some(BigDecimal(45)), Some(false))

        val journey: Journey = Journey(
          Some(SessionId(TdAll.sessionId).value),
          TdAll.journeyId,
          AuditFlags(),
          JourneyTypes.RefundJourney,
          Some(testAmount),
          Some(Nino("AA111111A")),
          None,
          None,
          Some(AccountType("Business")),
          Some(BankAccountInfo("Jon Smith", SortCode("111111"), AccountNumber("12345678"))),
          None,
          Some(true),
          Some(RepaymentResponse(OffsetDateTime.parse("2023-12-01T17:35:30+01:00"), RequestNumber("1234567890"))),
          None
        )

        "affinity is Individual" when {
          "users last payment was by card" should {
            "set the amount via the backend and redirect to /how-you-will-get-the-refund" in {
              stubBarsVerifyStatus()
              stubBackendJourneyId()
              stubPOSTJourney()
              stubBackendBusinessJourney(journey.nino)
              stubBackendLastPaymentMethod(Card)

              val result = amountController.submitAmount(fakeRequestWithValidFormPartialAmount)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/how-you-will-get-the-refund")

              verifyUpdateJourneyCalled(journey)
            }
          }

          "users last payment was by BACS or P0" should {
            "set the amount via the backend and redirect to /we-need-your-bank-details" in {
              stubBarsVerifyStatus()
              stubBackendJourneyId()
              stubPOSTJourney()
              stubBackendBusinessJourney(journey.nino)
              stubBackendLastPaymentMethod(BACS)

              val result = amountController.submitAmount(fakeRequestWithValidFormPartialAmount)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/we-need-your-bank-details")

              verifyUpdateJourneyCalled(journey)
            }
          }
        }

        "affinity is Agent" when {
          "users last payment was by card" should {
            "set the amount via the backend and redirect to /how-your-client-will-get-the-refund" in {
              stubBarsVerifyStatus()
              AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
              stubBackendJourneyId()
              stubPOSTJourney()
              stubBackendBusinessJourney(journey.nino)
              stubBackendLastPaymentMethod(Card)

              val result = amountController.submitAmount(fakeRequestWithValidFormPartialAmount)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/how-your-client-will-get-the-refund")

              verifyUpdateJourneyCalled(journey)
            }
          }

          "users last payment was by BACS or P0" should {
            "set the amount via the backend and redirect to /we-need-your-clients-bank-details" in {
              stubBarsVerifyStatus()
              AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
              stubBackendJourneyId()
              stubPOSTJourney()
              stubBackendBusinessJourney(journey.nino)
              stubBackendLastPaymentMethod(BACS)

              val result = amountController.submitAmount(fakeRequestWithValidFormPartialAmount)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/we-need-your-clients-bank-details")

              verifyUpdateJourneyCalled(journey)
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

    "display 'there is a problem' page if amount is missing" in {
      val fakeRequestWithValidFormFullAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "full")

      stubBackendJourneyId()
      stubPOSTJourney()
      stubBackendBusinessJourney(amount = None)

      val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)

      result.checkPageIsDisplayed(
        expectedHeading     = "Sorry, there is a problem with the service",
        expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
        journey             = "request",
        expectedStatus      = Status.INTERNAL_SERVER_ERROR,
        withBackButton      = false
      )
    }

    "display 'there is a problem' page if totalCreditAvailableForRepayment in amount is missing" in {
      val fakeRequestWithValidFormFullAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "full")

      stubBackendJourneyId()
      stubPOSTJourney()
      stubBackendBusinessJourney(amount = Some(testAmount.copy(totalCreditAvailableForRepayment = None)))

      val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)

      result.checkPageIsDisplayed(
        expectedHeading     = "Sorry, there is a problem with the service",
        expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
        journey             = "request",
        expectedStatus      = Status.INTERNAL_SERVER_ERROR,
        withBackButton      = false
      )
    }

    "display 'there is a problem' page if unallocatedCredit in amount is missing" in {
      val fakeRequestWithValidFormFullAmount = fakeRequestBase
        .withFormUrlEncodedBody("nino" -> "AA999999A", "fullAmount" -> "123", "choice" -> "full")

      stubBackendJourneyId()
      stubPOSTJourney()
      stubBackendBusinessJourney(amount = Some(testAmount.copy(unallocatedCredit = None)))

      val result = amountController.submitAmount(fakeRequestWithValidFormFullAmount)

      result.checkPageIsDisplayed(
        expectedHeading     = "Sorry, there is a problem with the service",
        expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
        journey             = "request",
        expectedStatus      = Status.INTERNAL_SERVER_ERROR,
        withBackButton      = false
      )
    }
  }
}
