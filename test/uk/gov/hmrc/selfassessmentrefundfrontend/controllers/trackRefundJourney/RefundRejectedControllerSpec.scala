/*
 * Copyright 2024 HM Revenue & Customs
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
import org.jsoup.nodes.Document
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.mvc.Http.Status
import support.{ItSpec, PageContentTesting, TestMessagesApiProvider}
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.RepaymentsConnector.Response
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll.{nino, no1, request, welshRequest}

import scala.concurrent.Future

class RefundRejectedControllerSpec extends ItSpec with PageContentTesting {

  val controller: RefundRejectedController = fakeApplication().injector.instanceOf[RefundRejectedController]

  class TestSetup(isAgent: Boolean = false) {
    if (isAgent) givenTheUserIsAuthorisedAsAgent() else givenTheUserIsAuthorised()

    stubBackendPersonalJourney(Some(nino))
    stubBarsVerifyStatus()
  }

  "onPageLoad" should {
    "render 'Refund Rejected' page with correct content" in new TestSetup() {
      givenTheRejectedRefundExists(no1, nino, 12000)

      val result: Future[Result] = controller.onPageLoad(no1)(request)

      result.checkPageIsDisplayed(
        expectedHeading     = "Your refund of £12,000 has been rejected",
        expectedServiceLink = "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker",
        contentChecks       = checkRejectedPageContent(welsh = false, agent = false),
        expectedStatus      = Status.OK,
        journey             = "track"
      )
    }

    "render 'Refund Rejected' page with correct content - in Welsh" in new TestSetup() {
      givenTheRejectedRefundExists(no1, nino, 12000)

      val result: Future[Result] = controller.onPageLoad(no1)(welshRequest)

      result.checkPageIsDisplayed(
        expectedHeading     = "Mae’ch ad-daliad o £12,000 wedi’i wrthod",
        expectedServiceLink = "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker",
        contentChecks       = checkRejectedPageContent(welsh = true, agent = false),
        expectedStatus      = Status.OK,
        journey             = "track",
        welsh               = true
      )
    }

    "render 'Refund Rejected' page with correct content for Agent" in new TestSetup(isAgent = true) {
      givenTheRejectedRefundExists(no1, nino, 12000)

      //The standard fakeApplication has an overridden AuthConnector that returns an Individual affinityGroup
      val refundControllerAuth: RefundRejectedController = new GuiceApplicationBuilder()
        .configure(
          "metrics.jvm" -> false,
          "metrics.enabled" -> false,
          "microservice.services.self-assessment-refund-backend.port" -> wireMockServer.port(),
          "microservice.services.auth.port" -> wireMockServer.port(),
          "microservice.services.bank-account-reputation.port" -> wireMockServer.port()
        )
        .overrides(bind[MessagesApi].toProvider[TestMessagesApiProvider])
        .build()
        .injector.instanceOf[RefundRejectedController]

      val result: Future[Result] = refundControllerAuth.onPageLoad(no1)(request)

      result.checkPageIsDisplayed(
        expectedHeading     = "Your refund of £12,000 has been rejected",
        expectedServiceLink = "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker",
        contentChecks       = checkRejectedPageContent(welsh = false, agent = true),
        expectedStatus      = Status.OK,
        journey             = "track"
      )
    }

    "render 'Refund Rejected' page with correct content for Agent - in Welsh" in new TestSetup(isAgent = true) {
      givenTheRejectedRefundExists(no1, nino, 12000)

      //The standard fakeApplication has an overridden AuthConnector that returns an Individual affinityGroup
      val refundControllerAuth: RefundRejectedController = new GuiceApplicationBuilder()
        .configure(
          "metrics.jvm" -> false,
          "metrics.enabled" -> false,
          "microservice.services.self-assessment-refund-backend.port" -> wireMockServer.port(),
          "microservice.services.auth.port" -> wireMockServer.port(),
          "microservice.services.bank-account-reputation.port" -> wireMockServer.port()
        )
        .overrides(bind[MessagesApi].toProvider[TestMessagesApiProvider])
        .build()
        .injector.instanceOf[RefundRejectedController]

      val result: Future[Result] = refundControllerAuth.onPageLoad(no1)(welshRequest)

      result.checkPageIsDisplayed(
        expectedHeading     = "Mae’ch ad-daliad o £12,000 wedi’i wrthod",
        expectedServiceLink = "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker",
        contentChecks       = checkRejectedPageContent(welsh = true, agent = true),
        expectedStatus      = Status.OK,
        journey             = "track",
        welsh               = true
      )
    }

    "render the error page if the Rejected refund exists but doesn't include a completed date" in new TestSetup() {
      stubFor(get(urlEqualTo(s"/self-assessment-refund-backend/repayments/${nino.value}/${no1.value}"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.prettyPrint(Json.toJson(Response(no1, nino, 12000, "Rejected", "2021-08-14", None, None, Some("PO")))))))

      val result: Future[Result] = controller.onPageLoad(no1)(request)

      result.checkPageIsDisplayed(
        expectedHeading          = "Sorry, there is a problem with the service",
        expectedServiceLink      = "",
        expectedStatus           = Status.INTERNAL_SERVER_ERROR,
        expectedTitleIfDifferent = Some("Sorry, there is a problem with the service - 500"),
        withBackButton           = false
      )
    }

    "redirect to error page if GET repayments returned error" in new TestSetup() {
      stubGetRepaymentError()

      val result: Future[Result] = controller.onPageLoad(no1)(request)

      result.checkPageIsDisplayed(
        expectedHeading          = "Sorry, there is a problem with the service",
        expectedServiceLink      = "",
        expectedStatus           = Status.INTERNAL_SERVER_ERROR,
        expectedTitleIfDifferent = Some("Sorry, there is a problem with the service - 500"),
        withBackButton           = false
      )
    }
  }

  def givenTheRejectedRefundExists(key: RequestNumber, nino: Nino, cashAmount: Long): StubMapping = {
    stubFor(get(urlEqualTo(s"/self-assessment-refund-backend/repayments/${nino.value}/${key.value}"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.prettyPrint(Json.toJson(Response(key, nino, cashAmount, "Rejected", "2021-08-14", Some("2021-08-31"), None))))))
  }

  private def checkRejectedPageContent(welsh: Boolean, agent: Boolean)(doc: Document): Unit = {
    val tryAgainLink = if (agent) "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund" else "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund"

    if (welsh) {
      doc.checkHasParagraphs(rejectedParagraphsWelsh)
      doc.checkHasHyperlink("roi cynnig arall ar wneud cais am ad-daliad", tryAgainLink)
      doc.checkHasHyperlink("A yw’r dudalen hon yn gweithio’n iawn? (yn agor tab newydd)", "/contact/report-technical-problem?service=self-assessment-repayment")
    } else {
      doc.checkHasParagraphs(rejectedParagraphs)
      doc.checkHasHyperlink("try again to request a refund", tryAgainLink)
      doc.checkHasHyperlink("Is this page not working properly? (opens in new tab)", "/contact/report-technical-problem?service=self-assessment-repayment")
    }

    doc.checkHasBackLinkWithUrl("#")
  }

  private val rejectedParagraphs: List[String] = List(
    "Refund reference: 1",
    "We rejected your request for a refund of £12,000.",
    "If you have credit in your online account, you can try again to request a refund."
  )

  private val rejectedParagraphsWelsh: List[String] = List(
    "Cyfeirnod yr ad-daliad: 1",
    "Gwnaethom wrthod eich cais am ad-daliad o £12,000.",
    "Os oes gennych gredyd yn eich cyfrif ar-lein, gallwch roi cynnig arall ar wneud cais am ad-daliad."
  )
}
