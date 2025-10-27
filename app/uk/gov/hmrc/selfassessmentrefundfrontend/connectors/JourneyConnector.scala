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

package uk.gov.hmrc.selfassessmentrefundfrontend.connectors

import cats.implicits.catsSyntaxEq
import com.google.inject.{Inject, Singleton}
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{Journey, JourneyId}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest.{StartRefund, ViewHistory}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.{PaymentMethod, ReplyURL}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyConnector @Inject() (servicesConfig: ServicesConfig, http: HttpClientV2) {

  private val baseUrl = servicesConfig.baseUrl("self-assessment-refund-backend")

  // This is a test only route to mimic View and Change calling the self-assessment-refund-backend
  def start(req: StartRequest, origin: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ReplyURL] = {
    val startUrl = {
      val intent = req match {
        case _: StartRefund => "start-refund"
        case _: ViewHistory => "view-history"
      }

      s"$baseUrl/self-assessment-refund-backend/$origin/journey/$intent"
    }

    http
      .post(url"$startUrl")
      .withBody(Json.toJson(req))
      .execute[ReplyURL]
  }

  def findLatestBySessionId()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Journey] = {
    val url = s"$baseUrl/self-assessment-refund-backend/journey/find-latest-by-session-id"
    http
      .get(url"$url")
      .execute[Journey]
  }

  def lastPaymentMethod(
    journeyId: JourneyId
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[PaymentMethod] = {
    val url = s"$baseUrl/self-assessment-refund-backend/last-payment/${journeyId.value}"

    http
      .get(url"$url")
      .execute[Option[PaymentMethod]]
      .map {
        case Some(method) => method
        case _            => throw new RuntimeException("LastPaymentOnCard network error")
      }
  }

  def setJourney(journeyId: JourneyId, value: Journey)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Unit] = {
    val url = s"$baseUrl/self-assessment-refund-backend/journey/${journeyId.value}"
    http
      .post(url"$url")
      .withBody(Json.toJson(value))
      .execute[HttpResponse]
      .map { response =>
        if (response.status === OK) ()
        else sys.error(s"call to set journey failed with status ${response.status.toString}")
      }
  }

}
