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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.{AuditOutcome, IVOutcomeAuditDetail}
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.bars._
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.startclaimjourney.RefundAmountAuditDetail
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.trackrefunds._
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.response.{BarsError, VerifyResponse}
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.model.BarsVerifyStatusResponse
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{JourneyType, JourneyTypes}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.{AccountType, BankAccountInfo}
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService.TaxRepayment
import uk.gov.hmrc.selfassessmentrefundfrontend.util.RequestSupport

object DataEventFactory {
  def barsCheckEvent(
      bankDetails:          BankAccountInfo,
      barsResponse:         Either[BarsError, VerifyResponse],
      verifyStatusResponse: BarsVerifyStatusResponse,
      maybeAccountType:     Option[AccountType],
      affinityGroup:        Option[AffinityGroup],
      maybeNino:            Option[Nino]
  )(implicit request: Request[_]): ExtendedDataEvent = {

    val detail = BarsCheckAuditDetail(
      maybeNino.fold("")(_.value),
      auditUserEnteredDetails(bankDetails, maybeAccountType),
      auditBarsOutcome(barsResponse, verifyStatusResponse),
      affinityGroup
    )

    makeExtendedDataEvent(
      auditType       = "BARSCheck",
      detail          = Json.toJson(detail),
      transactionName = "BARSCheck"
    )
  }

  def viewRefundStatusEvent(
      maybeTaxRepayments: Option[List[TaxRepayment]],
      affinityGroup:      Option[AffinityGroup],
      maybeNino:          Option[Nino],
      journeyType:        JourneyType,
      failureReason:      Option[String]
  )(implicit request: Request[_]): ExtendedDataEvent = {

    val detail = ViewRefundStatusAuditDetail(
      outcome  = AuditOutcome.fromFailureReason(failureReason),
      origin   = journeyType match {
        case JourneyTypes.TrackJourney =>
          "view and change"
        case JourneyTypes.RefundJourney =>
          "claim journey"
      },
      nino     = maybeNino,
      userType = affinityGroup,
      refunds  = maybeTaxRepayments.fold[List[RefundAuditDetail]](List.empty) { taxRepayments =>
        RefundAuditDetail.fromTaxRepayment(taxRepayments)
      }
    )

    makeExtendedDataEvent(
      auditType       = "ViewRefundStatus",
      detail          = Json.toJson(detail),
      transactionName = "ViewRefundStatus"
    )
  }

  def startClaimJourneyEvent(
      balanceDueWithin30Days: Option[BigDecimal],
      amountAvailable:        Option[BigDecimal],
      amountChosen:           Option[BigDecimal],
      affinityGroup:          Option[AffinityGroup],
      maybeNino:              Option[Nino],
      failureReason:          Option[String]
  )(implicit request: Request[_]): ExtendedDataEvent = {
    val detail = RefundAmountAuditDetail(
      outcome                = AuditOutcome.fromFailureReason(failureReason),
      balanceDueWithin30Days = balanceDueWithin30Days,
      amountAvailable        = amountAvailable,
      amountChosen           = amountChosen,
      nino                   = maybeNino,
      userType               = affinityGroup
    )

    makeExtendedDataEvent(
      auditType       = "RefundAmount",
      detail          = Json.toJson(detail),
      transactionName = "RefundAmount"
    )
  }

  def identityVerificationOutcomeEvent(isSuccessful: Boolean, maybeNino: Option[Nino], affinityGroup: Option[String])(implicit request: Request[_]): ExtendedDataEvent = {
    val detail = IVOutcomeAuditDetail(
      isSuccessful = isSuccessful,
      nino         = maybeNino,
      userType     = affinityGroup
    )

    makeExtendedDataEvent(
      auditType       = "IdentityVerificationOutcome",
      detail          = Json.toJson(detail),
      transactionName = "IdentityVerificationOutcome"
    )
  }

  private def auditUserEnteredDetails(bankDetails: BankAccountInfo, maybeAccountType: Option[AccountType]): BarsUserEnteredDetails =
    BarsUserEnteredDetails(
      maybeAccountType.fold("")(_.name),
      bankDetails.name,
      bankDetails.sortCode.value,
      bankDetails.accountNumber.value,
      bankDetails.rollNumber.map(_.value)
    )

  private def auditBarsOutcome(barsResponse: Either[BarsError, VerifyResponse], verifyStatusResponse: BarsVerifyStatusResponse): BarsVerifyOutcome = {
    BarsVerifyOutcome(
      isBankAccountValid    = barsResponse.isRight,
      unsuccessfulAttempts  = verifyStatusResponse.attempts.value,
      lockoutExpiryDateTime = verifyStatusResponse.lockoutExpiryDateTime.map(_.toString),
      barsResults           = barsResponse
    )
  }

  private def makeExtendedDataEvent(
      auditType:       String,
      detail:          JsValue,
      transactionName: String
  )(implicit request: Request[_]): ExtendedDataEvent = {

    val hc = RequestSupport.hc
    ExtendedDataEvent(
      auditSource = "self-assessment-refund-frontend",
      auditType   = auditType,
      detail      = detail,
      tags        = hc.toAuditTags(transactionName, request.uri) ++ Map(hc.names.deviceID -> hc.deviceID.getOrElse("-"))
    )
  }

}
