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
import org.jsoup.nodes.Document
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import support.ItSpec
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.RepaymentsConnector.Response
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.RepaymentStatusPageTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll._

import scala.concurrent.Future
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport._

class RepaymentStatusControllerSpec extends ItSpec with RepaymentStatusPageTesting {

  val controller: RepaymentStatusController = fakeApplication().injector.instanceOf[RepaymentStatusController]

  trait AuthorisedUserFixture {
    givenTheUserIsAuthorised()

    stubBackendPersonalJourney(Some(nino))
    stubBarsVerifyStatus()
  }

  val cashAmount = 12000
  val request1: FakeRequest[AnyContentAsEmpty.type] = request

  "the repayment status controller" when {
    "a repayment with status ProcessingRisking is requested" should {
      "redirect to the refund is being processed status page" in new AuthorisedUserFixture {
        givenTheProcessingRiskingRepaymentExists(no2, nino, cashAmount)

        val processingRiskingResult: Future[Result] = controller.statusOf(no2)(request1)

        status(processingRiskingResult) shouldBe 303
        redirectLocation(processingRiskingResult).value shouldBe trackRefundJourney.routes.RefundProcessingController.onPageLoad(no2).url
      }
    }

    "a repayment with status Processing is requested" should {
      "redirect to the refund is being processed status page" in new AuthorisedUserFixture {
        givenTheProcessingRepaymentExists(no2, nino, cashAmount)

        val processingResult: Future[Result] = controller.statusOf(no2)(request1)

        status(processingResult) shouldBe 303
        redirectLocation(processingResult).value shouldBe trackRefundJourney.routes.RefundProcessingController.onPageLoad(no2).url
      }

      "a rejected repayment is requested" should {
        "return the rejected status page" in new AuthorisedUserFixture {
          givenTheRejectedRepaymentExists(no2, nino, cashAmount)

          val rejectedResult: Future[Result] = controller.statusOf(no2)(request1)

          status(rejectedResult) shouldBe 303
          redirectLocation(rejectedResult).value shouldBe trackRefundJourney.routes.RefundRejectedController.onPageLoad(no2).url
        }
      }

      "a paid repayment is requested" should {
        "return the paid status page" in new AuthorisedUserFixture {
          givenTheApprovedRepaymentExists(no2, nino, cashAmount)

          val paidResult = controller.statusOf(no2)(request1)

          status(paidResult) shouldBe 303
          redirectLocation(paidResult).value shouldBe trackRefundJourney.routes.RefundApprovedController.showApprovedPage(no2).url

        }
      }
      "the backend is unavailable" should {
        "return an internal server error" in new AuthorisedUserFixture {
          val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/track-a-self-assessment-refund/refund-status")
            .withAuthToken()
            .withRequestId()
            .withSessionId()

          stubRepaymentWithNumber()

          val pageUnavailableResponse: Future[Result] = controller.statusOf(no2)(fakeRequest)
          val doc: Document = Jsoup.parse(contentAsString(pageUnavailableResponse))

          status(pageUnavailableResponse) shouldBe 500
          doc.title() shouldBe "Sorry, there is a problem with the service - Track a Self Assessment refund - GOV.UK"
          doc.select("h1").text() shouldBe "Sorry, there is a problem with the service"
          doc.select("body").text should include("Sorry, there is a problem with the service Try again later. Contact HMRC if you need to speak to someone about your Self Assessment refund.")
          doc.checkHasHyperlink("Contact HMRC", "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment")
        }
      }
    }

      def givenTheProcessingRiskingRepaymentExists(key: RequestNumber, nino: Nino, cashAmount: Long): StubMapping = {
        stubFor(getRepayments(key).willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.prettyPrint(Json.toJson(Response(key, nino, cashAmount, "ProcessingRisking", "2021-08-14"))))))
      }

      def givenTheProcessingRepaymentExists(key: RequestNumber, nino: Nino, cashAmount: Long): StubMapping = {
        stubFor(getRepayments(key).willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.prettyPrint(Json.toJson(Response(key, nino, cashAmount, "Processing", "2021-08-14"))))))
      }

      def givenTheRejectedRepaymentExists(key: RequestNumber, nino: Nino, cashAmount: Long): StubMapping = {
        stubFor(getRepayments(key).willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.prettyPrint(Json.toJson(Response(key, nino, cashAmount, "Rejected", "2021-08-14", Some("2021-08-31"), None))))))
      }

      def givenTheApprovedRepaymentExists(key: RequestNumber, nino: Nino, cashAmount: Long): StubMapping = {
        stubFor(getRepayments(key).willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.prettyPrint(Json.toJson(Response(key, nino, cashAmount, "Approved", "2021-08-14", Some("2021-08-31")))))))
      }

      def getRepayments(key: RequestNumber) = get(urlEqualTo(s"/self-assessment-refund-backend/repayments/${nino.value}/${key.value}"))
  }

}
