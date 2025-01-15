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

import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.{AnyContentAsEmpty, Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import support.ItSpec
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.{SessionId, SessionKeys}
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.AuditFlags
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{Journey, JourneyId, JourneyTypes}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.CheckYourAnswersPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.CheckYourAnswersPage
import uk.gov.hmrc.selfassessmentrefundfrontend.views.refundrequestjourney.CheckYourAnswersHelper
import java.time.OffsetDateTime

import support.stubbing.AuthStub
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

import scala.concurrent.Future

class CheckYourAnswersPageControllerSpec extends ItSpec with CheckYourAnswersPageTesting {

  val controller: CheckYourAnswersPageController = fakeApplication().injector.instanceOf[CheckYourAnswersPageController]
  val cyaViewHelper: CheckYourAnswersHelper = fakeApplication().injector.instanceOf[CheckYourAnswersHelper]

  override def beforeEach(): Unit = {
    super.beforeEach()
    AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
    ()
  }

  override def fakeAuthConnector: Option[AuthConnector] = None

  trait JourneyFixture {
    val sessionId: SessionId = SessionId(TdAll.sessionId)
    val journeyId: JourneyId = TdAll.journeyId
    val nino: Nino = Nino("AA111111A")
  }

  trait RequestWithSessionFixture extends JourneyFixture {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.sessionId -> sessionId.value).withAuthToken()
  }

  trait RequestWithSessionFixtureWelsh extends JourneyFixture {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      .withSession(SessionKeys.sessionId -> sessionId.value)
      .withCookies(Cookie("PLAY_LANG", "cy"))
      .withAuthToken()
  }

  val headers: Map[String, JsString] = Map[String, JsString](
    "Host" -> JsString("localhost")
  )
  val createRepaymentRequest: CreateRepaymentRequest = CreateRepaymentRequest(
    headerData = new JsObject(headers)
  )

  "check your answers controller" when {
    "called on start" when {
      "showing the content of the page" should {
        "send a copy of the html page to the backend" in new RequestWithSessionFixture {
          stubBarsVerifyStatus()
          stubBackendPersonalJourney(Some(nino))
          stubPOSTJourney()
          val response: Future[Result] = controller.start(request)

          response.checkPageIsDisplayed(
            expectedHeading     = "Check your answers",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks       = checkPageContent("Personal", BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678")), 123),
            expectedStatus      = OK,
            journey             = "request"
          )

          val page: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]
          val summaryList: SummaryList = cyaViewHelper.buildSummaryList(Amount(Some(123), None, None, Some(123), Some(123)), AccountType.Personal, BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678")))
          val html: HtmlFormat.Appendable = page(
            summaryList,
            routes.CheckYourAnswersPageController.confirm,
            false
          )(request, app.injector.instanceOf[MessagesApi].preferred(request))

          val journey: Journey = Journey(
            Some(sessionId.value),
            journeyId,
            AuditFlags(),
            JourneyTypes.RefundJourney,
            Some(testAmount),
            Some(nino),
            None,
            None,
            Some(AccountType("Personal")),
            Some(BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678"))),
            Some(html.toString()),
            None,
            Some(RepaymentResponse(OffsetDateTime.parse("2023-12-01T17:35:30+01:00"), RequestNumber("1234567890"))),
            None
          )

          verifyUpdateJourneyCalled(journey)
        }
        "send a copy of the agent html page to the backend" in new RequestWithSessionFixture {
          AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
          stubBarsVerifyStatus()
          stubBackendPersonalJourney(Some(nino))
          stubPOSTJourney()
          val response: Future[Result] = controller.start(request)

          response.checkPageIsDisplayed(
            expectedHeading     = "Check your answers",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund",
            contentChecks       = checkPageContent("Personal", BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678")), 123),
            expectedStatus      = OK,
            journey             = "request"
          )

          val page: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]
          val summaryList: SummaryList = cyaViewHelper.buildSummaryList(Amount(Some(123), None, None, Some(123), Some(123)), AccountType.Personal, BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678")))
          val html: HtmlFormat.Appendable = page(
            summaryList,
            routes.CheckYourAnswersPageController.confirm,
            true
          )(request, app.injector.instanceOf[MessagesApi].preferred(request))

          val journey: Journey = Journey(
            Some(sessionId.value),
            journeyId,
            AuditFlags(),
            JourneyTypes.RefundJourney,
            Some(testAmount),
            Some(nino),
            None,
            None,
            Some(AccountType("Personal")),
            Some(BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678"))),
            Some(html.toString()),
            None,
            Some(RepaymentResponse(OffsetDateTime.parse("2023-12-01T17:35:30+01:00"), RequestNumber("1234567890"))),
            None
          )

          verifyUpdateJourneyCalled(journey)
        }
        "send a copy of the welsh html page to the backend" in new RequestWithSessionFixtureWelsh {
          stubBarsVerifyStatus()
          stubBackendPersonalJourney(Some(nino))
          stubPOSTJourney()
          val response: Future[Result] = controller.start(request)

          response.checkPageIsDisplayed(
            expectedHeading     = "Gwiriwch eich atebion",
            expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
            contentChecks       = checkPageContentWelsh("Personol", BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678")), 123),
            expectedStatus      = OK,
            journey             = "request",
            welsh               = true
          )

          val page: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]
          val summaryList: SummaryList = cyaViewHelper.buildSummaryList(Amount(Some(123), None, None, Some(123), Some(123)), AccountType.Personal, BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678")))
          val html: HtmlFormat.Appendable = page(
            summaryList,
            routes.CheckYourAnswersPageController.confirm,
            false
          )(request, app.injector.instanceOf[MessagesApi].preferred(request))

          val journey: Journey = Journey(
            Some(sessionId.value),
            journeyId,
            AuditFlags(),
            JourneyTypes.RefundJourney,
            Some(testAmount),
            Some(nino),
            None,
            None,
            Some(AccountType("Personal")),
            Some(BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678"))),
            Some(html.toString()),
            None,
            Some(RepaymentResponse(OffsetDateTime.parse("2023-12-01T17:35:30+01:00"), RequestNumber("1234567890"))),
            None
          )

          verifyUpdateJourneyCalled(journey)
        }
      }
    }

  }
}
