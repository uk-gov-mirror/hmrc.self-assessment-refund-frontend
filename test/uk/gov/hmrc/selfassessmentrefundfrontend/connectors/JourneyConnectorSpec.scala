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

package uk.gov.hmrc.selfassessmentrefundfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import support.ItSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.ReplyURL
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest.ViewHistory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneyConnectorSpec extends ItSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val journeyConnector: JourneyConnector = fakeApplication().injector.instanceOf[JourneyConnector]
  val startRefundRequest: StartRequest.StartRefund = StartRequest.StartRefund("AA999999A", BigDecimal(987.611), None, None)
  val viewHistoryRequest: ViewHistory = ViewHistory("AA999999A")
  val replyUrl: ReplyURL = ReplyURL(nextUrl = "http://localhost:1234")

  def startUrl(isHistory: Boolean): String = {
    val intent = if (isHistory) "view-history" else "start-refund"
    s"/self-assessment-refund-backend/testOrigin/journey/$intent"
  }

  "start" should {
    "return a ReplyURL for the start refund journey" in {
      stubFor(post(urlEqualTo(startUrl(isHistory = false)))
        .willReturn(aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(replyUrl).toString())))

      val result: Future[ReplyURL] = journeyConnector.start(startRefundRequest, "testOrigin")
      result.futureValue shouldBe ReplyURL("http://localhost:1234")
    }

    "return a ReplyURL for the view history journey" in {
      stubFor(post(urlEqualTo(startUrl(isHistory = true)))
        .willReturn(aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(replyUrl).toString())))

      val result: Future[ReplyURL] = journeyConnector.start(viewHistoryRequest, "testOrigin")
      result.futureValue shouldBe ReplyURL("http://localhost:1234")
    }

    "handle exceptions" in {
      stubFor(post(urlEqualTo(startUrl(isHistory = false)))
        .willReturn(aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
          .withBody("Internal server error")))

      val error = intercept[UpstreamErrorResponse](await(journeyConnector.start(startRefundRequest, "testOrigin")))
      error.statusCode shouldBe INTERNAL_SERVER_ERROR
      error.message should include("Internal server error")
    }
  }
}
