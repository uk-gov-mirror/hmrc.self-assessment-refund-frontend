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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ItSpec
import uk.gov.hmrc.selfassessmentrefundfrontend.TdRepayments
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.RepaymentsConnector.Response
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.RefundTrackerPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

import scala.concurrent.Future

class RefundTrackerControllerSpec extends ItSpec with TdRepayments with RefundTrackerPageTesting {

  private val refundTrackerController = app.injector.instanceOf[RefundTrackerController]

  trait HistoryWithSessionFixture {
    val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/track-a-self-assessment-refund/refund-request-tracker")
      .withAuthToken()
      .withSessionId()

    val fakeRequestWelsh: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/track-a-self-assessment-refund/refund-request-tracker")
      .withAuthToken()
      .withSessionId()
      .withCookies(Cookie("PLAY_LANG", "cy"))

    givenTheUserIsAuthorised()
    givenRepaymentsExistForNino(nino)
    stubBackendPersonalJourney(Some(nino))
    stubBarsVerifyStatus()
  }

  "the refund tracker controller" when {
    "called on 'startRefundTracker'" should {
      "redirect to GET /refund-request-tracker" in new HistoryWithSessionFixture {
        stubBackendPersonalJourney()
        val result: Future[Result] = refundTrackerController.startRefundTracker(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("/track-a-self-assessment-refund/refund-request-tracker")
      }
    }
    "called on 'refundTracker'" when {

      "the user does not have a refund history" should {
        "display an error page page" in {
          val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/track-a-self-assessment-refund/refund-request-tracker")
            .withAuthToken()
            .withSessionId()

          givenTheUserIsAuthorised()

          val repaymentsTwo: List[Response] = List()
          stubFor(get(urlEqualTo(s"/self-assessment-refund-backend/repayments/${nino.value}"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(Json.prettyPrint(Json.toJson(repaymentsTwo)))))
          stubBackendPersonalJourney(Some(nino))
          stubBarsVerifyStatus()
          stubBackendBusinessJourney(Some(Nino("AA111111A")))

          val result: Future[Result] = refundTrackerController.refundTracker()(fakeRequest)

          val doc = Jsoup.parse(contentAsString(result))

          doc.title() shouldBe "Sorry, there is a problem with the service - 500 - GOV.UK"
          doc.select("h1").text() shouldBe "Sorry, there is a problem with the service"
          doc.select("body").text should include("Sorry, there is a problem with the service Try again later.")
        }

      }

      "the user has a session and a refund history" should {

        "display 'refund tracker' page" in new HistoryWithSessionFixture {
          stubBackendBusinessJourney(Some(Nino("AA111111A")))
          val result: Future[Result] = refundTrackerController.refundTracker()(fakeRequest)

          result.checkPageIsDisplayed(
            expectedHeading     = "Refund request tracker",
            expectedServiceLink = "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker",
            contentChecks       = checkPageContent,
            expectedStatus      = Status.OK,
            journey             = "track"
          )
        }

        "display welsh 'refund tracker' page" in new HistoryWithSessionFixture {
          stubBackendBusinessJourney(Some(Nino("AA111111A")))
          val result: Future[Result] = refundTrackerController.refundTracker()(fakeRequestWelsh)

          result.checkPageIsDisplayed(
            expectedHeading     = "System olrhain ceisiadau am ad-daliad",
            expectedServiceLink = "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker",
            contentChecks       = checkPageContentWelsh,
            expectedStatus      = Status.OK,
            journey             = "track",
            welsh               = true
          )
        }

      }

      "the user does not have a current session" should {
        "redirect users to login" in {
          val fakeRequest = FakeRequest("GET", "/track-a-self-assessment-refund/refund-request-tracker")
            .withAuthToken()

          givenTheUserIsAuthorised()
          stubBackendBusinessJourney(Some(Nino("AA111111A")))
          val result = refundTrackerController.refundTracker()(fakeRequest)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/gg-sign-in?continue=http://localhost:9171/self-assessment-refund/self-assessment-refund/test-only")
        }
      }
    }
  }

  def givenRepaymentsExistForNino(nino: Nino): StubMapping = {
    val repayments: List[Response] = List(
      Response(
        key             = no1,
        nino            = nino,
        payment         = 12000,
        status          = "Processing",
        created         = "2021-08-14",
        completed       = None,
        rejection       = None,
        repaymentMethod = Some("Card")
      ),
      Response(
        key             = no2,
        nino            = nino,
        payment         = 76000,
        status          = "Approved",
        created         = "2021-08-16",
        completed       = Some("2021-08-17"),
        rejection       = None,
        repaymentMethod = Some("BACS")
      ),
    )

    stubFor(get(urlEqualTo(s"/self-assessment-refund-backend/repayments/${nino.value}"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.prettyPrint(Json.toJson(repayments)))))
  }
}
