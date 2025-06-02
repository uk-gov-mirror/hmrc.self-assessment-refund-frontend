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

package uk.gov.hmrc.selfassessmentrefundfrontend.services

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.AuditService
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.{JourneyConnector, RepaymentsConnector}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.AuthenticatedRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.{RepaymentStatus, RequestNumber}
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService.TaxRepayment
import uk.gov.hmrc.selfassessmentrefundfrontend.util.RequestSupport

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod

@Singleton
class RepaymentsService @Inject() (
    auditService:        AuditService,
    journeyConnector:    JourneyConnector,
    repaymentsConnector: RepaymentsConnector
)(implicit ec: ExecutionContext) extends Logging {

  def repayments(nino: Nino)(implicit request: AuthenticatedRequest[_]): Future[List[TaxRepayment]] = {
    import RequestSupport.hc

    logger.debug(s"retrieving TaxRepayments for nino ${nino.value}")
    val resp =
      repaymentsConnector.taxPayerRepayments(nino)
        .map { taxRepayments =>
          logger.debug(s"""returned tax repayments ${taxRepayments.mkString(", ")}""")

          if (!request.journey.audit.hasSentViewRefund) {
            auditService.auditViewRefundStatus(
              Some(taxRepayments),
              Some(request.affinityGroup),
              request.journey.nino,
              request.agentReferenceNumber,
              request.journey.journeyType
            )

            val auditFlags = request.journey.audit.copy(hasSentViewRefund = true)
            val _ = journeyConnector.setJourney(request.journey.id, request.journey.copy(audit = auditFlags))
          }

          taxRepayments
        }

    resp
  }

  def repayment(nino: Nino, number: RequestNumber)(implicit hc: HeaderCarrier): Future[TaxRepayment] = {
    logger.debug(s"retrieving TaxRepayment with key ${number.value} and nino ${nino.value}")

    repaymentsConnector.taxPayerRepayment(nino, number).map { tr =>
      logger.debug(s"returned tax repayment ${tr.toString}")
      tr
    }
  }

}

object RepaymentsService {

  final case class Claim(
      key:             RequestNumber,
      nino:            Nino,
      amount:          BigDecimal,
      created:         LocalDate,
      repaymentMethod: Option[PaymentMethod]
  )

  sealed trait TaxRepayment {
    def claim: Claim
    def status: RepaymentStatus
  }

  final case class ProcessingRiskingTaxRepayment(claim: Claim) extends TaxRepayment {
    def status: RepaymentStatus = RepaymentStatus.ProcessingRisking
  }

  final case class ProcessingTaxRepayment(claim: Claim) extends TaxRepayment {
    def status: RepaymentStatus = RepaymentStatus.Processing
  }

  final case class ApprovedTaxRepayment(claim: Claim, completed: LocalDate) extends TaxRepayment {
    def status: RepaymentStatus = RepaymentStatus.Approved
  }

  final case class RejectedTaxRepayment(claim: Claim, completed: LocalDate, message: Option[String] = None) extends TaxRepayment {
    def status: RepaymentStatus = RepaymentStatus.Rejected
  }
}
