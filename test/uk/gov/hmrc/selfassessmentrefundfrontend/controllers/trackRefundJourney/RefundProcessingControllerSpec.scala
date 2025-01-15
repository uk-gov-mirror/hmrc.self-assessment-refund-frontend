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
import play.api.libs.json.Json
import play.api.mvc.Result
import play.mvc.Http.Status
import support.{ItSpec, PageContentTesting}
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.RepaymentsConnector.Response
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll.{nino, no1, request}

import scala.concurrent.Future

class RefundProcessingControllerSpec extends ItSpec with PageContentTesting {

  val controller: RefundProcessingController = fakeApplication().injector.instanceOf[RefundProcessingController]

  trait AuthorisedUserFixture {
    givenTheUserIsAuthorised()

    stubBackendPersonalJourney(Some(nino))
    stubBarsVerifyStatus()
  }

  "onPageLoad" should {
    "render 'Refund being processes' page with correct content" in new AuthorisedUserFixture {
      givenTheProcessingRefundExists(no1, nino, 12000)

      val result: Future[Result] = controller.onPageLoad(no1)(request)

      result.checkPageIsDisplayed(
        expectedHeading     = "Your refund of £12,000 is being processed",
        expectedServiceLink = "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker",
        contentChecks       = checkProcessingPageContent,
        expectedStatus      = Status.OK,
        journey             = "track"
      )
    }

    "redirect to error page if GET repayments returned error" in new AuthorisedUserFixture {
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

  def givenTheProcessingRefundExists(key: RequestNumber, nino: Nino, cashAmount: Long): StubMapping = {
    stubFor(get(urlEqualTo(s"/self-assessment-refund-backend/repayments/${nino.value}/${key.value}"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.prettyPrint(Json.toJson(Response(key, nino, cashAmount, "Processing", "2021-08-14"))))))
  }

  private def checkProcessingPageContent(doc: Document): Unit = {

    doc.checkHasParagraphs(processingParagraphs)

    doc.checkHasHyperlink("contact us", "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment")

    doc.checkHasBackLinkWithUrl("#")
  }

  private val processingParagraphs: List[String] = List(
    "Refund reference: 1",
    "We received your refund request on 14 August 2021.",
    "You should get your refund by 21 September 2021. However, there are security measures in place which may cause a delay.",
    "If you have not received your refund by 21 September 2021, you can contact us. Do not contact us before this date as we will not have an update for you."
  )
}
