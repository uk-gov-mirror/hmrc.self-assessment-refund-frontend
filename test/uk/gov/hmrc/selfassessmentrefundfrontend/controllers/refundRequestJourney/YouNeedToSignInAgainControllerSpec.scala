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
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{AnyContentAsEmpty, Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.stubbing.{AuditStub, AuthStub}
import support.{ItSpec, WireMockSupport}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.http.{SessionId, SessionKeys}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.CreateRepaymentRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.JourneyId
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.YouNeedToSignInAgainPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

import scala.concurrent.Future

class YouNeedToSignInAgainControllerSpec
    extends ItSpec
    with GuiceOneAppPerSuite
    with WireMockSupport
    with YouNeedToSignInAgainPageTesting {
  that: TestSuite =>

  private val youNeedToSignInAgainController = app.injector.instanceOf[YouNeedToSignInAgainController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
    ()
  }

  override def fakeAuthConnector: Option[AuthConnector] = None

  private val PageHeading      = "For your security, you need to sign in again"
  private val PageHeadingWelsh = "Er eich diogelwch, mae angen i chi fewngofnodi eto"

  @SuppressWarnings(Array("org.wartremover.warts.AnyVal"))
  override lazy val configOverrides: Map[String, Any] = Map(
    "auditing.enabled"               -> true,
    "auditing.consumer.baseUri.port" -> wireMockServer.port
  )

  val headers: Map[String, JsString]                 = Map[String, JsString](
    "Host" -> JsString("localhost")
  )
  val createRepaymentRequest: CreateRepaymentRequest = CreateRepaymentRequest(
    headerData = new JsObject(headers)
  )

  trait JourneyFixture {
    val sessionId: SessionId = SessionId(TdAll.sessionId)
    val journeyId: JourneyId = TdAll.journeyId
    val nino: Nino           = Nino("AA111111A")
  }

  trait RequestWithSessionFixture extends JourneyFixture {
    val request: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest().withSession(SessionKeys.sessionId -> sessionId.value).withAuthToken()
  }

  "GET /sign-in-again" when {
    val fakeRequest = FakeRequest("GET", "/request-a-self-assessment-refund/sign-in-again")
      .withSessionId()
      .withAuthToken()

    "called" should {
      "display 'you will need to sign in again' page with correct content" in {
        stubBackendJourneyId()
        stubBackendPersonalJourney()

        val result: Future[Result] = youNeedToSignInAgainController.onPageLoad(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading = PageHeading,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks = checkPageContent,
          expectedStatus = Status.OK,
          journey = "request"
        )
      }
      "display 'you will need to sign in again' page with correct content for Agents" in {
        AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
        stubBackendJourneyId()
        stubBackendPersonalJourney()

        val result: Future[Result] = youNeedToSignInAgainController.onPageLoad(fakeRequest)

        result.checkPageIsDisplayed(
          expectedHeading = PageHeading,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
          contentChecks = checkPageContent,
          expectedStatus = Status.OK,
          journey = "request"
        )
      }
      "display welsh 'you will need to sign in again' page with correct content" in {
        stubBackendJourneyId()
        stubBackendPersonalJourney()

        val result: Future[Result] =
          youNeedToSignInAgainController.onPageLoad(fakeRequest.withCookies(Cookie("PLAY_LANG", "cy")))

        result.checkPageIsDisplayed(
          expectedHeading = PageHeadingWelsh,
          expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
          contentChecks = checkPageContentWelsh,
          expectedStatus = Status.OK,
          journey = "request",
          welsh = true
        )
      }
    }
  }

  "POST /sign-in-again" when {
    val fakeRequest = FakeRequest("POST", "/request-a-self-assessment-refund/sign-in-again")
      .withSessionId()
      .withAuthToken()

    "called" should {
      "redirect to /request-a-self-assessment-refund/reauthentication" in {
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
        stubBackendPersonalJourney()

        val result: Future[Result] = youNeedToSignInAgainController.onSubmit(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/reauthentication")
      }
    }
  }

  "GET /reauthenticated-submit" when {
    "a journey id is found" when {
      "the hasStartedReauth flag has not been set" should {
        "redirect to GET /request-a-self-assessment-refund/refund-request-not-submitted" in new RequestWithSessionFixture {
          stubBackendBusinessJourney(Some(nino), hasStartedReauth = None, repaymentResponse = None)
          stubBarsVerifyStatus()

          val response = youNeedToSignInAgainController.reauthSuccessful(request)
          status(response) shouldBe SEE_OTHER
          redirectLocation(response) shouldBe Some(s"/request-a-self-assessment-refund/refund-request-not-submitted")

          AuditStub.verifyEventAudited(
            "RefundRequest",
            Json
              .parse(
                """
              |{
              |  "etmpResult": "Fail",
              |  "userType": "Individual",
              |  "totalCreditAvailableForRepayment": "123.00",
              |  "unallocatedCredit": "45.00",
              |  "amountChosen": "123.00",
              |  "nino": "AA111111A",
              |  "nrsSubmissionId": "MissingNrsSubmissionId",
              |  "bankAccount": {
              |    "accountType": "Business",
              |    "accountHolderName": "Jon Smith",
              |    "sortCode": "111111",
              |    "accountNumber": "12345678"
              |  }
              |}""".stripMargin
              )
              .as[JsObject]
          )
        }
      }

      "the hasStartedReauth flag is false" should {
        "redirect to GET /request-a-self-assessment-refund/refund-request-not-submitted" in new RequestWithSessionFixture {
          stubBackendBusinessJourney(Some(nino), hasStartedReauth = Some(false), repaymentResponse = None)
          stubBarsVerifyStatus()

          val response = youNeedToSignInAgainController.reauthSuccessful(request)
          status(response) shouldBe SEE_OTHER
          redirectLocation(response) shouldBe Some(s"/request-a-self-assessment-refund/refund-request-not-submitted")

          AuditStub.verifyEventAudited(
            "RefundRequest",
            Json
              .parse(
                """
              |{
              |  "etmpResult": "Fail",
              |  "userType": "Individual",
              |  "totalCreditAvailableForRepayment": "123.00",
              |  "unallocatedCredit": "45.00",
              |  "amountChosen": "123.00",
              |  "nino": "AA111111A",
              |  "nrsSubmissionId": "MissingNrsSubmissionId",
              |  "bankAccount": {
              |    "accountType": "Business",
              |    "accountHolderName": "Jon Smith",
              |    "sortCode": "111111",
              |    "accountNumber": "12345678"
              |  }
              |}""".stripMargin
              )
              .as[JsObject]
          )
        }
      }

      "hasStartedReauth flag is true and repayment has been created via backend" should {
        "redirect to GET /refund-request-confirmation/:journeyId" in new RequestWithSessionFixture {
          stubBackendBusinessJourney(Some(nino), hasStartedReauth = Some(true))
          stubBarsVerifyStatus()
          stubCreateRepayment(createRepaymentRequest)

          val response = youNeedToSignInAgainController.reauthSuccessful(request)
          status(response) shouldBe SEE_OTHER
          redirectLocation(response) shouldBe Some(
            s"/request-a-self-assessment-refund/refund-request-received/${TdAll.no1.value}"
          )

          AuditStub.verifyEventAudited(
            "RefundRequest",
            Json
              .parse(
                """
              |{
              |  "etmpResult": "Success",
              |  "userType": "Individual",
              |  "totalCreditAvailableForRepayment": "123.00",
              |  "unallocatedCredit": "45.00",
              |  "amountChosen": "123.00",
              |  "reference": "1234567890",
              |  "nino": "AA111111A",
              |  "nrsSubmissionId": "submissionId",
              |  "bankAccount": {
              |    "accountType": "Business",
              |    "accountHolderName": "Jon Smith",
              |    "sortCode": "111111",
              |    "accountNumber": "12345678"
              |  }
              |}""".stripMargin
              )
              .as[JsObject]
          )
        }

        "redirect to GET /refund-request-confirmation/:journeyId (RepaymentCreatedResponse with no nrsSubmissionId variation)" in new RequestWithSessionFixture {
          stubBackendBusinessJourney(Some(nino), hasStartedReauth = Some(true))
          stubBarsVerifyStatus()
          stubCreateRepaymentNoNRSSubmissionId(createRepaymentRequest)

          val response = youNeedToSignInAgainController.reauthSuccessful(request)
          status(response) shouldBe SEE_OTHER
          redirectLocation(response) shouldBe Some(
            s"/request-a-self-assessment-refund/refund-request-received/${TdAll.no1.value}"
          )

          AuditStub.verifyEventAudited(
            "RefundRequest",
            Json
              .parse(
                """
              |{
              |  "etmpResult": "Success",
              |  "userType": "Individual",
              |  "totalCreditAvailableForRepayment": "123.00",
              |  "unallocatedCredit": "45.00",
              |  "amountChosen": "123.00",
              |  "reference": "1234567890",
              |  "nino": "AA111111A",
              |  "nrsSubmissionId": "MissingNrsSubmissionId",
              |  "bankAccount": {
              |    "accountType": "Business",
              |    "accountHolderName": "Jon Smith",
              |    "sortCode": "111111",
              |    "accountNumber": "12345678"
              |  }
              |}""".stripMargin
              )
              .as[JsObject]
          )
        }
      }

      "a repayment fails to be created via backend" should {
        "redirect to GET /request-a-self-assessment-refund/refund-request-not-submitted" in new RequestWithSessionFixture {
          stubBackendBusinessJourney(Some(nino), hasStartedReauth = Some(true), repaymentResponse = None)
          stubBarsVerifyStatus()

          stubCreateRepaymentError(createRepaymentRequest)

          val response = youNeedToSignInAgainController.reauthSuccessful(request)
          status(response) shouldBe SEE_OTHER
          redirectLocation(response) shouldBe Some(s"/request-a-self-assessment-refund/refund-request-not-submitted")

          AuditStub.verifyEventAudited(
            "RefundRequest",
            Json
              .parse(
                """
              |{
              |  "etmpResult": "Fail",
              |  "userType": "Individual",
              |  "totalCreditAvailableForRepayment": "123.00",
              |  "unallocatedCredit": "45.00",
              |  "amountChosen": "123.00",
              |  "nino": "AA111111A",
              |  "nrsSubmissionId": "MissingNrsSubmissionId",
              |  "bankAccount": {
              |    "accountType": "Business",
              |    "accountHolderName": "Jon Smith",
              |    "sortCode": "111111",
              |    "accountNumber": "12345678"
              |  }
              |}""".stripMargin
              )
              .as[JsObject]
          )
        }
      }
    }
  }
}
