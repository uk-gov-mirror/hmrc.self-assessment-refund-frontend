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
import play.api.mvc.{Cookie, Result}
import play.api.test.Helpers.{call, defaultAwaitTimeout, redirectLocation, status, writeableOf_AnyContentAsFormUrlEncoded}
import play.api.test.{FakeRequest, Helpers}
import support.ItSpec
import support.stubbing.AuthStub
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.AuditFlags
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.AccountTypeController.AccountTypeEnum
import uk.gov.hmrc.selfassessmentrefundfrontend.model.{AccountType, BankAccountInfo}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod.Card
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{Journey, JourneyTypes}
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.AccountTypePageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

import scala.concurrent.Future

class AccountTypeControllerSpec extends ItSpec with AccountTypePageTesting {

  val controller: AccountTypeController = fakeApplication().injector.instanceOf[AccountTypeController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
    ()
  }

  def journey(accountType: AccountType, bankAccountInfo: Option[BankAccountInfo]): Journey = Journey(
    sessionId = Some(TdAll.sessionId),
    id = TdAll.journeyId,
    audit = AuditFlags(),
    journeyType = JourneyTypes.RefundJourney,
    amount = Some(testAmount),
    nino = None,
    mtdItId = None,
    paymentMethod = None,
    accountType = Some(accountType),
    bankAccountInfo = bankAccountInfo,
    nrsWebpage = None,
    hasStartedReauth = Some(true),
    repaymentConfirmation = Some(testRepaymentResponse),
    returnUrl = None
  )

  override def fakeAuthConnector: Option[AuthConnector] = None

  "the account type controller" when {
    "called on getAccountType" when {
      "the current conversation has no cached data" should {
        "return the account type page" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(method = Some(Card))
          stubBarsVerifyStatus()

          val response = controller.getAccountType(TdAll.request)

          response.checkPageIsDisplayed(
            expectedHeading = "What type of bank account are you providing?",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks = checkPageContent,
            expectedStatus = Status.OK,
            journey = "request"
          )
        }

        "return the account type page for an Agent" in {
          AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
          stubBackendJourneyId()
          stubBackendBusinessJourney(method = Some(Card))
          stubBarsVerifyStatus()

          val response = controller.getAccountType(TdAll.request)

          response.checkPageIsDisplayed(
            expectedHeading = "What type of bank account are you providing?",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
            contentChecks = checkPageContent,
            expectedStatus = Status.OK,
            journey = "request"
          )
        }

        "return the account type page in welsh" in {
          stubBackendJourneyId()
          stubBackendBusinessJourney(method = Some(Card))
          stubBarsVerifyStatus()

          val response = controller.getAccountType(TdAll.welshRequest)

          response.checkPageIsDisplayed(
            expectedHeading = "Pa fath o gyfrif banc ydych chi’n ei roi?",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks = checkPageContentWelsh,
            expectedStatus = Status.OK,
            journey = "request",
            welsh = true
          )
        }
      }

      "the current conversation has the account type in its cache" should {
        "return the account type page" in {
          stubBackendBusinessJourney(method = Some(Card))
          stubBackendJourneyId()
          stubBarsVerifyStatus()

          val response = controller.getAccountType(TdAll.request)

          response.checkPageIsDisplayed(
            expectedHeading = "What type of bank account are you providing?",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks = checkPageContent,
            expectedStatus = Status.OK,
            journey = "request"
          )
        }
      }
    }

    "called on postAccountType" when {
      "a journey id is found" when {
        "a valid form is submitted" should {
          "update the conversation cache" in {
            val request                  = FakeRequest(Helpers.POST, routes.AccountTypeController.postAccountType.path())
              .withSessionId()
              .withAuthToken()
              .withFormUrlEncodedBody("accountType" -> AccountTypeEnum.Business.toString)
            val action                   = controller.postAccountType()
            stubPOSTJourney()
            stubBackendBusinessJourney()
            val response: Future[Result] = call(action, request, request.body)
            status(response) shouldBe Status.SEE_OTHER
            redirectLocation(response) shouldBe Some("/request-a-self-assessment-refund/bank-building-society-details")

            verifyUpdateJourneyCalled(
              journey(
                accountType = AccountType.Business,
                bankAccountInfo = None
              )
            )
          }

          "redirect back to the CYA Page if the user didn't change their answer (Business)" in {
            val request                  = FakeRequest(Helpers.POST, routes.AccountTypeController.postAccountType.path())
              .withSessionId()
              .withAuthToken()
              .withFormUrlEncodedBody("accountType" -> AccountTypeEnum.Business.toString)
              .withSession("self-assessment-refund.changing-account-from-cya-page" -> "redirectToCYA")
            val action                   = controller.postAccountType()
            stubBackendBusinessJourney()
            stubPOSTJourney()
            val response: Future[Result] = call(action, request, request.body)
            status(response) shouldBe Status.SEE_OTHER
            redirectLocation(response) shouldBe Some("/request-a-self-assessment-refund/check-your-answers")

            verifyUpdateJourneyCalled(
              journey(
                accountType = AccountType.Business,
                bankAccountInfo = Some(TdAll.bankAccountInfo)
              )
            )
          }

          "Don't redirect back to the CYA Page if the user changed their answer (Business -> Personal)" in {
            val request                  = FakeRequest(Helpers.POST, routes.AccountTypeController.postAccountType.path())
              .withSessionId()
              .withAuthToken()
              .withFormUrlEncodedBody("accountType" -> AccountTypeEnum.Personal.toString)
              .withSession("self-assessment-refund.changing-account-from-cya-page" -> "redirectToCYA")
            val action                   = controller.postAccountType()
            stubBackendBusinessJourney()
            stubPOSTJourney()
            val response: Future[Result] = call(action, request, request.body)
            status(response) shouldBe Status.SEE_OTHER
            redirectLocation(response) shouldBe Some("/request-a-self-assessment-refund/bank-building-society-details")

            verifyUpdateJourneyCalled(
              journey(
                accountType = AccountType.Personal,
                bankAccountInfo = None
              )
            )
          }
        }
        "the submitted form has errors" should {
          "remain on the page" in {
            val request                  = FakeRequest(Helpers.POST, routes.AccountTypeController.postAccountType.path())
              .withSessionId()
              .withAuthToken()
              .withFormUrlEncodedBody("accountType" -> "anError")
            val action                   = controller.postAccountType()
            stubBackendBusinessJourney()
            val response: Future[Result] = call(action, request, request.body)
            status(response) shouldBe Status.BAD_REQUEST

            response.checkPageIsDisplayed(
              expectedHeading = "What type of bank account are you providing?",
              expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
              contentChecks = checkPageWithFormError,
              expectedStatus = Status.BAD_REQUEST,
              withError = true,
              journey = "request"
            )
          }
          "remain on the page in welsh" in {
            val request                  = FakeRequest(Helpers.POST, routes.AccountTypeController.postAccountType.path())
              .withSessionId()
              .withAuthToken()
              .withFormUrlEncodedBody("accountType" -> "anError")
              .withCookies(Cookie("PLAY_LANG", "cy"))
            val action                   = controller.postAccountType()
            stubBackendBusinessJourney()
            val response: Future[Result] = call(action, request, request.body)
            status(response) shouldBe Status.BAD_REQUEST

            response.checkPageIsDisplayed(
              expectedHeading = "Pa fath o gyfrif banc ydych chi’n ei roi?",
              expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
              contentChecks = checkPageWithFormErrorWelsh,
              expectedStatus = Status.BAD_REQUEST,
              withError = true,
              journey = "request",
              welsh = true
            )
          }
        }
      }
    }
  }

}
