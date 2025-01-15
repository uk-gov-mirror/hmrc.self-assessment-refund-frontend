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

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.selfassessmentrefundfrontend.config.AppConfig
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.RepaymentsConnector.Response
import uk.gov.hmrc.selfassessmentrefundfrontend.model.CreateRepaymentRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.JourneyId
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.{RepaymentCreatedResponse, RequestNumber, parseLocalDate}
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService._
import uk.gov.hmrc.selfassessmentrefundfrontend.util.Mapping
import uk.gov.hmrc.selfassessmentrefundfrontend.util.Mapping._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepaymentsConnector @Inject() (client: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext) {

  val baseUrl: String = s"${config.selfAssessmentRepaymentBackendUrl}/self-assessment-refund-backend"

  def createRepayment(journeyId: JourneyId, headerData: JsObject)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, RepaymentCreatedResponse]] = {
    val url = s"$baseUrl/repayments/${journeyId.value}"

    val requestBody = CreateRepaymentRequest(headerData)

    client.post(url"$url")
      .withBody(Json.toJson(requestBody))
      .execute[Either[UpstreamErrorResponse, RepaymentCreatedResponse]]
  }

  def taxPayerRepayments(nino: Nino)(implicit hc: HeaderCarrier): Future[List[TaxRepayment]] = {
    val url = s"$baseUrl/repayments/${nino.value}"

    val repaymentsListFuture = client.get(url"$url")
      .execute[List[Response]]
      .map(_.mapTo[List[TaxRepayment]])

    repaymentsListFuture.map(list => if (list.isEmpty) throw new Exception("unexpected empty list of tax repayments") else list)
  }

  def taxPayerRepayment(nino: Nino, number: RequestNumber)(implicit hc: HeaderCarrier): Future[TaxRepayment] = {
    val url = s"$baseUrl/repayments/${nino.value}/${number.value}"

    client.get(url"$url")
      .execute[Response]
      .map(_.mapTo[TaxRepayment])
  }

}

object RepaymentsConnector {

  final case class Response(
      key:             RequestNumber,
      nino:            Nino,
      payment:         BigDecimal,
      status:          String,
      created:         String,
      completed:       Option[String] = None,
      rejection:       Option[String] = None,
      repaymentMethod: Option[String] = None
  )

  object Response {

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    implicit val format: OFormat[Response] = Json.format[Response]

    implicit val conv: Mapping[Response, TaxRepayment] = (response: Response) => {
      @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable", "org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
      val maybePaymentMethod = response.repaymentMethod.map(_.toUpperCase) match {
        case Some("CARD") => Some(PaymentMethod.Card)
        case Some("BACS") => Some(PaymentMethod.BACS)
        case Some("PO")   => Some(PaymentMethod.PaymentOrder)
        case _            => None
      }

      response.status match {
        case "ProcessingRisking" => ProcessingRiskingTaxRepayment(Claim(response.key, response.nino, response.payment, parseLocalDate(response.created), maybePaymentMethod))
        case "Processing"        => ProcessingTaxRepayment(Claim(response.key, response.nino, response.payment, parseLocalDate(response.created), maybePaymentMethod))

        case "Approved" => ApprovedTaxRepayment(
          Claim(response.key, response.nino, response.payment, parseLocalDate(response.created), maybePaymentMethod),
          response.completed.map(parseLocalDate).getOrElse(throw new IllegalArgumentException("missing completion date"))
        ) // throw an exception for now since the real data types are unknown

        case "Rejected" =>
          RejectedTaxRepayment(
            Claim(response.key, response.nino, response.payment, parseLocalDate(response.created), maybePaymentMethod),
            response.completed.map(parseLocalDate).getOrElse(throw new IllegalArgumentException("missing completion date"))
          )

      }
    }
  }

}
