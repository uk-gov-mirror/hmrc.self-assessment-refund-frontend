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

package uk.gov.hmrc.selfassessmentrefundfrontend.audit

import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.{BankAccountDetailsAudit, RepaymentRequestAuditItem}
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.response.{BarsError, VerifyResponse}
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.model.BarsVerifyStatusResponse
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{Journey, JourneyType}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.{AccountType, BankAccountInfo}
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService.TaxRepayment
import uk.gov.hmrc.selfassessmentrefundfrontend.util.ApplicationLogging

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) extends ApplicationLogging {

  def auditBarsCheck(
      bankDetails:          BankAccountInfo,
      result:               Either[BarsError, VerifyResponse],
      verifyStatusResponse: BarsVerifyStatusResponse,
      maybeAccountType:     Option[AccountType],
      affinityGroup:        Option[AffinityGroup],
      maybeNino:            Option[Nino],
      maybeArn:             Option[String]
  )(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.barsCheckEvent(bankDetails, result, verifyStatusResponse, maybeAccountType, affinityGroup, maybeNino, maybeArn)

    sendExtendedEvent(event)
  }

  def auditRefundRequestEvent(
      journey:         Journey,
      nrsSubmissionId: Option[String],
      affinityGroup:   Option[String],
      maybeArn:        Option[String]
  )(implicit hc: HeaderCarrier): Unit = {
    try {
      val totalCreditAvailableForRepayment = journey.amount.map(_.totalCreditAvailableForRepayment.getOrElse(BigDecimal("0"))).getOrElse(BigDecimal("0")).setScale(2)
      val unallocatedCredit = journey.amount.map(_.unallocatedCredit.getOrElse(BigDecimal("0"))).getOrElse(BigDecimal("0")).setScale(2)
      val amountChosen = journey.amount.map(_.repay).getOrElse(BigDecimal("0")).setScale(2)

      val repaymentRequestAuditItem = RepaymentRequestAuditItem(
        etmpResult                       = journey.repaymentConfirmation.fold("Fail")(_ => "Success"),
        userType                         = affinityGroup.getOrElse("MissingAffinityGroup"),
        agentReferenceNumber             = maybeArn,
        totalCreditAvailableForRepayment = totalCreditAvailableForRepayment.toString(),
        unallocatedCredit                = unallocatedCredit.toString(),
        amountChosen                     = amountChosen.toString(),
        barsResponse                     = None,
        reference                        = journey.repaymentConfirmation.map(_.repaymentRequestNumber.value),
        nino                             = journey.nino.map(_.value).getOrElse("MissingNino"),
        nrsSubmissionId                  = nrsSubmissionId.getOrElse("MissingNrsSubmissionId"),
        bankAccount                      = BankAccountDetailsAudit.fromOptionalBankAccountInfo(journey.accountType, journey.bankAccountInfo)
      )
      val event = ExtendedDataEvent(
        auditSource = "self-assessment-refund-frontend",
        auditType   = "RefundRequest",
        detail      = Json.toJsObject(repaymentRequestAuditItem),
        tags        = AuditExtensions.auditHeaderCarrier(hc).toAuditTags()
      )

      sendExtendedEvent(event)
    } catch {
      case e: Throwable =>
        logger.error("[AuditService][auditRefundRequestEvent] Unable to create audit: " + e.getMessage)
    }
  }

  def auditViewRefundStatus(
      taxRepayments: Option[List[TaxRepayment]],
      affinityGroup: Option[AffinityGroup],
      maybeNino:     Option[Nino],
      maybeArn:      Option[String],
      journeyType:   JourneyType,
      failureReason: Option[String]             = None
  )(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.viewRefundStatusEvent(taxRepayments, affinityGroup, maybeNino, maybeArn, journeyType, failureReason)

    sendExtendedEvent(event)
  }

  def auditRefundAmount(
      totalCreditAvailableForRepayment: Option[BigDecimal],
      unallocatedCredit:                Option[BigDecimal],
      amountChosen:                     Option[BigDecimal],
      affinityGroup:                    Option[AffinityGroup],
      maybeNino:                        Option[Nino],
      maybeArn:                         Option[String],
      failureReason:                    Option[String]        = None
  )(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.startClaimJourneyEvent(totalCreditAvailableForRepayment, unallocatedCredit, amountChosen, affinityGroup, maybeNino, maybeArn, failureReason)

    sendExtendedEvent(event)
  }

  def auditIVOutcome(isSuccessful: Boolean, maybeNino: Option[Nino], affinityGroup: Option[String])(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.identityVerificationOutcomeEvent(isSuccessful, maybeNino, affinityGroup)

    sendExtendedEvent(event)
  }

  private def sendExtendedEvent(event: ExtendedDataEvent): Unit = {
    val checkEventResult = auditConnector.sendExtendedEvent(event)
    checkEventResult.onComplete {
      case Success(value)       => logger.info(s"Send audit event outcome: audit event ${event.auditType} successfully posted - ${value.toString}")
      case Failure(NonFatal(e)) => logger.warn(s"Send audit event outcome: unable to post audit event of type ${event.auditType} to audit connector", e)
      case _                    => logger.info(s"Send audit event outcome: Event audited ${event.auditType}")
    }
  }

}
