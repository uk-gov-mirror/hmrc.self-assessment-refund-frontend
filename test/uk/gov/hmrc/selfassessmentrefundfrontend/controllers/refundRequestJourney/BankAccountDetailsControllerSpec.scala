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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers.{await, call, defaultAwaitTimeout, redirectLocation, status, writeableOf_AnyContentAsFormUrlEncoded}
import play.api.test.{FakeRequest, Helpers}
import support.ItSpec
import support.stubbing.BarsJsonResponses.{ValidateJson, VerifyJson}
import support.stubbing.BarsStub
import uk.gov.hmrc.http.{SessionId, SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.JourneyId
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.BankAccountDetailsPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll

import scala.concurrent.Future

class BankAccountDetailsControllerSpec extends ItSpec with BankAccountDetailsPageTesting {

  val controller: BankAccountDetailsController = fakeApplication().injector.instanceOf[BankAccountDetailsController]

  trait JourneyFixture {
    val sessionId: SessionId = SessionId(TdAll.sessionId)
    val journeyId: JourneyId = TdAll.journeyId
    val nino: Nino = Nino("AA111111A")
  }

  trait RequestWithSessionFixture extends JourneyFixture {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.sessionId -> sessionId.value)
  }

  trait RequestWithSessionFixtureWelsh extends JourneyFixture {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      .withSession(SessionKeys.sessionId -> sessionId.value)
      .withCookies(Cookie("PLAY_LANG", "cy"))
  }

  trait AccountFormFixture extends JourneyFixture {
    stubBarsVerifyStatus() // not locked out

    val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(Helpers.POST, refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails.path())
      .withSession(SessionKeys.sessionId -> sessionId.value)
      .withFormUrlEncodedBody("accountName" -> "D Jones", "sortCode" -> "12-23-34", "accountNumber" -> "12345678")

    def stubsForBarsError(): StubMapping = {
      stubBackendBusinessJourney()
    }
  }
  trait AccountFormFixtureWelsh extends JourneyFixture {
    stubBarsVerifyStatus() // not locked out

    val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(Helpers.POST, refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails.path())
      .withSession(SessionKeys.sessionId -> sessionId.value)
      .withFormUrlEncodedBody("accountName" -> "D Jones", "sortCode" -> "12-23-34", "accountNumber" -> "12345678")
      .withCookies(Cookie("PLAY_LANG", "cy"))

    def stubsForBarsError(): StubMapping = {
      stubBackendBusinessJourney()
    }
  }

  "the bank account details controller" when {
    "called on 'getAccountDetails" when {
      "a journey id is found" when {
        "the current journey has no cached data" should {
          "return the bank info page" in new RequestWithSessionFixture {
            givenTheBankAccountIsNotInTheCache(journeyId)
            stubBackendJourneyId()
            stubBackendBusinessJourney()

            val response: Future[Result] = controller.getAccountDetails(request)

            response.checkPageIsDisplayed(
              expectedHeading     = "Bank or building society account details",
              expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
              contentChecks       = checkPageContent,
              expectedStatus      = Status.OK,
              journey             = "request"
            )
          }

          "return the bank info page in welsh" in new RequestWithSessionFixtureWelsh {
            givenTheBankAccountIsNotInTheCache(journeyId)
            stubBackendJourneyId()
            stubBackendBusinessJourney()

            val response: Future[Result] = controller.getAccountDetails(request)

            response.checkPageIsDisplayed(
              expectedHeading     = "Manylion y cyfrif banc neu gymdeithas adeiladu",
              expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
              contentChecks       = checkPageContentWelsh,
              expectedStatus      = Status.OK,
              journey             = "request",
              welsh               = true
            )
          }
        }

        "the current journey has cached data" should {
          "return the bank info page" in new RequestWithSessionFixture {
            givenTheBankAccountIsInTheCache(journeyId)
            stubBackendJourneyId()
            stubBackendBusinessJourney()

            val response: Future[Result] = controller.getAccountDetails(request)

            response.checkPageIsDisplayed(
              expectedHeading     = "Bank or building society account details",
              contentChecks       = checkPageContent,
              expectedStatus      = Status.OK,
              expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
              journey             = "request"
            )
          }
        }
      }
    }

    "called on 'postAccountDetails'" when {
      "return the next page if bank details have not changed and are already verified" in new AccountFormFixture {
        // This version of request with account details matches cache stub details
        override val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(Helpers.POST, refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails.path())
          .withSession(SessionKeys.sessionId -> sessionId.value)
          .withFormUrlEncodedBody("accountName" -> "D Jones", "sortCode" -> "122334", "accountNumber" -> "12345678")

        givenTheBusinessAccountIsValid
        stubPOSTJourney()
        stubBackendBusinessJourney(Some(nino))
        val action: Action[AnyContent] = controller.postAccountDetails()
        val response: Future[Result] = call(action, request, request.body)

        status(response) shouldBe Status.SEE_OTHER
        redirectLocation(response) shouldBe Some("/request-a-self-assessment-refund/check-your-answers")
      }
      "return the next page on successful account validation" when {
        "it is a business account" in new AccountFormFixture {
          stubBackendBusinessJourney(Some(nino))

          givenTheBusinessAccountIsValid
          stubPOSTJourney()
          givenTheCacheUpdateSucceeds(journeyId)
          val action: Action[AnyContent] = controller.postAccountDetails()
          val response: Future[Result] = call(action, request, request.body)

          status(response) shouldBe Status.SEE_OTHER
          redirectLocation(response) shouldBe Some("/request-a-self-assessment-refund/check-your-answers")
        }
        "it is a personal account" in new AccountFormFixture {
          stubBackendPersonalJourney(Some(nino))

          givenThePersonalAccountIsValid
          stubPOSTJourney()
          givenTheCacheUpdateSucceeds(journeyId)
          val action: Action[AnyContent] = controller.postAccountDetails()
          val response: Future[Result] = call(action, request, request.body)

          status(response) shouldBe Status.SEE_OTHER
          redirectLocation(response) shouldBe Some("/request-a-self-assessment-refund/check-your-answers")
        }
      }

      "Form validation shows error" when {
        val validAccountName = "D Jones"
        val validSortCode = "12-23-34"
        val validAccountNumber = "12345678"

        val listOfValidationErrors = List(
          // (errorReason, errorMessage, accountName, sortCode, accountNumber, rollNumber, errorMessage, errorFieldSpan)
          ("account name is missing", "", validSortCode, validAccountNumber, "", "Enter the name on the account", "accountName"),
          ("account name is too long", "A" * 61, validSortCode, validAccountNumber, "", "Name on the account must be 60 characters or less", "accountName"),
          ("account name is invalid", "This±IsAn£Invalid/Name", validSortCode, validAccountNumber, "", "Name on the account must only include letters, hyphens, spaces and apostrophes", "accountName"),

          ("account number is missing", validAccountName, validSortCode, "", "", "Enter an account number", "accountNumber"),
          ("account number is too long", validAccountName, validSortCode, "1111111111", "", "Account number must be between 6 and 8 digits", "accountNumber"),
          ("account number is invalid", validAccountName, validSortCode, "123abc", "", "Account number must be between 6 and 8 digits", "accountNumber"),

          ("sort code is missing", validAccountName, "", validAccountNumber, "", "Enter a sort code", "sortCode"),
          ("sort code is too long", validAccountName, "12345678", validAccountNumber, "", "Sort code must be 6 digits", "sortCode"),
          ("sort code is invalid", validAccountName, "123abc", validAccountNumber, "", "Sort code must be 6 digits", "sortCode"),

          // Roll number is optional
          ("rollNumber is too long", validAccountName, validSortCode, validAccountNumber, "A" * 11, "Building society roll number must be between 1 and 10 characters", "rollNumber"),
          ("rollNumber is invalid", validAccountName, validSortCode, validAccountNumber, "a!bad%form", "Building society roll number must only include letters a to z, numbers and spaces", "rollNumber")
        )

        listOfValidationErrors.foreach {
          case (errorReason, accountName, sortCode, accountNumber, rollNumber, errorMessage, errorFieldSpan) =>
            s"$errorReason" should {
              s"return bad request with error message $errorMessage" in new JourneyFixture {
                stubBarsVerifyStatus() // not locked out
                stubBackendBusinessJourney()

                val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(Helpers.POST, refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails.path())
                  .withSession(SessionKeys.sessionId -> sessionId.value)
                  .withFormUrlEncodedBody(
                    "accountName" -> accountName,
                    "sortCode" -> sortCode,
                    "accountNumber" -> accountNumber,
                    "rollNumber" -> rollNumber
                  )

                val action: Action[AnyContent] = controller.postAccountDetails()
                val response: Future[Result] = call(action, request, request.body)

                response.checkPageIsDisplayed(
                  expectedHeading     = "Bank or building society account details",
                  expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
                  contentChecks       = checkPageWithFormError(errorMessage, errorFieldSpan, s"#$errorFieldSpan"),
                  expectedStatus      = Status.BAD_REQUEST,
                  withError           = true,
                  journey             = "request"
                )
              }
            }
        }
      }

      // TODO standard form validation errors (invalid sortCode, invalid accountNumber, accountName too long etc)
      "BARS Validate check fails" when {
        val listOfValidationErrors = List(
          ("account number is not well formatted", Status.OK, ValidateJson.accountNumberNotWellFormatted, "Enter a valid combination of sort code and account number", "bars-invalid"),
          ("sort code on deny list", Status.BAD_REQUEST, ValidateJson.sortCodeOnDenyList, "Enter a valid combination of sort code and account number", "bars-invalid"),
          ("sort code not present on EISCD", Status.OK, ValidateJson.sortCodeNotPresentOnEiscd, "Enter a valid combination of sort code and account number", "bars-invalid"),
          ("sort code does not support direct credit", Status.OK, ValidateJson.sortCodeDoesNotSupportsDirectDebit, "You have entered a sort code which does not accept refunds. Check you have entered a valid sort code or enter details for a different account", "sortCode")
        )

        listOfValidationErrors.foreach {
          case (errorReason, errorStatus, errorResponseBody, errorMessage, errorFieldSpan) =>
            s"$errorReason" should {
              s"return bad request with error message $errorMessage" in new AccountFormFixture {
                stubsForBarsError()
                BarsStub.ValidateStub.stubForPostWith(errorResponseBody, errorStatus)

                val action: Action[AnyContent] = controller.postAccountDetails()
                val response: Future[Result] = call(action, request, request.body)

                response.checkPageIsDisplayed(
                  expectedHeading     = "Bank or building society account details",
                  expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
                  contentChecks       = checkPageWithFormError(errorMessage, errorFieldSpan, "#sortCode"),
                  expectedStatus      = Status.BAD_REQUEST,
                  withError           = true,
                  journey             = "request"
                )
              }
            }

        }

      }

      "BARS Validate check fails for welsh page" when {
        val listOfValidationErrors = List(
          ("account number is not well formatted", Status.OK, ValidateJson.accountNumberNotWellFormatted, "Nodwch gyfuniad dilys ar gyfer y cod didoli a rhif y cyfrif", "bars-invalid"),
          ("sort code on deny list", Status.BAD_REQUEST, ValidateJson.sortCodeOnDenyList, "Nodwch gyfuniad dilys ar gyfer y cod didoli a rhif y cyfrif", "bars-invalid"),
          ("sort code not present on EISCD", Status.OK, ValidateJson.sortCodeNotPresentOnEiscd, "Nodwch gyfuniad dilys ar gyfer y cod didoli a rhif y cyfrif", "bars-invalid"),
          ("sort code does not support direct credit", Status.OK, ValidateJson.sortCodeDoesNotSupportsDirectDebit, "Rydych wedi nodi cod didoli nad yw’n derbyn ad-daliadau. Gwiriwch eich bod wedi nodi cod didoli dilys, neu nodwch fanylion ar gyfer cyfrif gwahanol", "sortCode")
        )

        listOfValidationErrors.foreach {
          case (errorReason, errorStatus, errorResponseBody, errorMessage, errorFieldSpan) =>
            s"$errorReason" should {
              s"return bad request with error message $errorMessage" in new AccountFormFixtureWelsh {
                stubsForBarsError()
                BarsStub.ValidateStub.stubForPostWith(errorResponseBody, errorStatus)

                val action: Action[AnyContent] = controller.postAccountDetails()
                val response: Future[Result] = call(action, request, request.body)

                response.checkPageIsDisplayed(
                  expectedHeading     = "Manylion y cyfrif banc neu gymdeithas adeiladu",
                  expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
                  contentChecks       = checkPageWithFormErrorWelsh(errorMessage, errorFieldSpan, "#sortCode"),
                  expectedStatus      = Status.BAD_REQUEST,
                  withError           = true,
                  journey             = "request",
                  welsh               = true
                )
              }
            }

        }

      }

      "BARS Verify check fails" when {
        val listOfVerifyErrors = List(
          ("account number not well formatted", VerifyJson.accountNumberNotWellFormatted, "Enter a valid combination of sort code and account number", "bars-invalid", "#sortCode"),
          ("sort code does not support direct credit", VerifyJson.sortCodeDoesNotSupportDirectCredit, "You have entered a sort code which does not accept refunds. Check you have entered a valid sort code or enter details for a different account", "sortCode", "#sortCode"),
          ("sort code not present on Eiscd", VerifyJson.sortCodeNotPresentOnEiscd, "Enter a valid combination of sort code and account number", "bars-invalid", "#sortCode"),
          //        ("sort code on deny list", ),
          ("name does not match", VerifyJson.nameDoesNotMatch, "Enter the name on the account as it appears on bank statements", "accountName", "#accountName"),
          ("account does not exist", VerifyJson.accountDoesNotExist, "Enter a valid combination of sort code and account number", "bars-invalid", "#sortCode"),
          ("other bars error", VerifyJson.otherBarsError, "Enter a valid combination of sort code and account number", "bars-invalid", "#sortCode"),
          ("non standard details required", VerifyJson.nonStandardDetailsRequired, "Building society roll number must be entered if you have one. It may also be called a reference code", "rollNumber", "#rollNumber")
        )

        listOfVerifyErrors.foreach {
          case (errorReason, errorResponseBody, errorMessage, errorFieldSpan, errorLink) =>
            s"$errorReason" should {
              s"return bad request with error message $errorMessage" in new AccountFormFixture {
                stubsForBarsError()
                BarsStub.ValidateStub.success()
                BarsStub.VerifyBusinessStub.stubForPostWith(errorResponseBody)

                val action: Action[AnyContent] = controller.postAccountDetails()
                val response: Future[Result] = call(action, request, request.body)

                response.checkPageIsDisplayed(
                  expectedHeading     = "Bank or building society account details",
                  expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
                  contentChecks       = checkPageWithFormError(errorMessage, errorFieldSpan, errorLink),
                  expectedStatus      = Status.BAD_REQUEST,
                  withError           = true,
                  journey             = "request"
                )
              }
            }
        }
      }

      "throw exception when BARS Verify check fails with 'Third party error'" in new AccountFormFixture {
        stubsForBarsError()
        BarsStub.ValidateStub.success()
        BarsStub.VerifyBusinessStub.stubForPostWith(VerifyJson.thirdPartyError)

        val action: Action[AnyContent] = controller.postAccountDetails()
        val response: Future[Result] = call(action, request, request.body)

        intercept[RuntimeException] {
          status(response)
        }.getMessage should include ("BARS verify third-party error. BARS response:")
      }

      "redirect to 'bars lockout' page if BARS verify fails a third time and locks out" in new JourneyFixture {
        stubBackendBusinessJourney(Some(nino))
        stubBackendAccountType()
        stubBackendAccountTypeWithNino()

        BarsStub.ValidateStub.success()
        BarsStub.VerifyBusinessStub.stubForPostWith(VerifyJson.accountDoesNotExist)
        stubBarsVerifyUpdateWithLockout()
        stubTwoBarsVerifyStatusFailedSecondWithLockout() // So that the first check as part of authenticated journey action is not a lock out, but the second, after the new verify check is a lockout

        val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(Helpers.POST, refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails.path())
          .withSession(SessionKeys.sessionId -> sessionId.value)
          .withFormUrlEncodedBody("accountName" -> "D Jones", "sortCode" -> "12-23-34", "accountNumber" -> "12345678")

        val action: Action[AnyContent] = controller.postAccountDetails()
        val response: Future[Result] = call(action, request, request.body)

        status(response) shouldBe Status.SEE_OTHER
        redirectLocation(response) shouldBe Some("/request-a-self-assessment-refund/bank-details-tried-too-many-times")
      }

      "return an error page when the banking verification service produces an internal service error" in new AccountFormFixture {
        stubBackendBusinessJourney(Some(nino))
        BarsStub.ValidateStub.success()
        BarsStub.VerifyBusinessStub.internalServerError()
        val action: Action[AnyContent] = controller.postAccountDetails()
        an[UpstreamErrorResponse] shouldBe thrownBy (await(call(action, request, request.body)))
      }

      "block the user from accessing the account checking service" in new AccountFormFixture {
        stubBackendPersonalJourney(Some(nino))
        stubPOSTJourney()
        givenThePersonalAccountIsValid
        val action: Action[AnyContent] = controller.postAccountDetails()
        val response: Future[Result] = call(action, request, request.body)
        status(response) shouldBe Status.SEE_OTHER
      }

      "stay on page and display validation error if form filled out incorrectly" in new JourneyFixture {
        val request: FakeRequest[AnyContentAsFormUrlEncoded] = {
          FakeRequest(Helpers.POST, refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails.path())
            .withSession(SessionKeys.sessionId -> sessionId.value)
            .withFormUrlEncodedBody()
        }

        stubBackendBusinessJourney()
        val action: Action[AnyContent] = controller.postAccountDetails()
        val response: Future[Result] = call(action, request, request.body)

        response.checkPageIsDisplayed(
          expectedHeading     = "Bank or building society account details",
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks       = checkPageWithFormError(Map(
            "accountName" -> "This field is required",
            "sortCode" -> "This field is required",
            "accountNumber" -> "This field is required",
          )),
          expectedStatus      = Status.BAD_REQUEST,
          withError           = true,
          journey             = "request"
        )

      }
    }

  }

  def givenTheBankAccountIsNotInTheCache(journeyId: JourneyId): StubMapping = stubFor(get(urlEqualTo(s"/self-assessment-refund-backend/bank-account/account-info/${journeyId.value}"))
    .willReturn(aResponse()
      .withStatus(404)))

  def givenTheBankAccountIsInTheCache(journeyId: JourneyId): StubMapping = stubFor(get(urlEqualTo(s"/self-assessment-refund-backend/bank-account/account-info/${journeyId.value}"))
    .willReturn(aResponse()
      .withStatus(200).withBody(Json.prettyPrint(Json.toJson(BankAccountInfo("D Jones", SortCode("sort-code"), AccountNumber("account-number")))))))

  def givenTheCacheUpdateSucceeds(journeyId: JourneyId): StubMapping = stubFor(put(urlEqualTo(s"/self-assessment-refund-backend/bank-account/account-info/${journeyId.value}"))
    .willReturn(aResponse.withStatus(200)))

  def givenTheBusinessAccountIsValid: StubMapping = {
    BarsStub.ValidateStub.success()
    BarsStub.VerifyBusinessStub.success()
  }

  def givenThePersonalAccountIsValid: StubMapping = {
    BarsStub.ValidateStub.success()
    BarsStub.VerifyPersonalStub.success()
  }

  def givenBarsAccessIsDenied: StubMapping =
    stubFor(post(urlEqualTo(s"/verify/business"))
      .willReturn(aResponse()
        .withStatus(403)))

  def givenThePersonalAccountDoesNotExist: StubMapping = {
    stubFor(post(urlEqualTo(s"/verify/personal"))
      .willReturn(aResponse()
        .withStatus(200)))
  }

  def givenTheNinoIsInTheCache(journeyId: JourneyId, nino: Nino): StubMapping =
    stubFor(get(urlEqualTo(s"/self-assessment-refund-backend/bank-account/account-info/${journeyId.value}/nino"))
      .willReturn(aResponse()
        .withStatus(200).withBody(Json.prettyPrint(Json.toJson(nino)))))

}
