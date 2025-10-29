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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action

import play.api.Logging
import play.api.mvc.{ActionRefiner, Request, Result, Results}
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.AuditService
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.BarsVerifyStatusConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.model.BarVerifyStatusId
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.{AuthenticatedRequest, BarsVerifiedRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BarsLockoutActionRefiner @Inject() (
  auditService:              AuditService,
  barsVerifyStatusConnector: BarsVerifyStatusConnector
)(implicit ec: ExecutionContext)
    extends ActionRefiner[AuthenticatedRequest, BarsVerifiedRequest]
    with Logging
    with Results {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, BarsVerifiedRequest[A]]] = {
    implicit val rh: Request[A] = request.request

    request.journey.nino match {
      case Some(nino) =>
        barsVerifyStatusConnector.status(BarVerifyStatusId.from(nino)).map { status =>
          status.lockoutExpiryDateTime match {
            case Some(_) =>
              val amount = request.journey.amount
              auditService.auditRefundAmount(
                totalCreditAvailableForRepayment = amount.flatMap(_.totalCreditAvailableForRepayment),
                unallocatedCredit = amount.flatMap(_.unallocatedCredit),
                amountChosen = amount.flatMap(_.repayment),
                affinityGroup = Some(request.affinityGroup),
                maybeNino = request.journey.nino,
                maybeArn = request.agentReferenceNumber,
                failureReason = Some("bars lockout")
              )

              Left(Redirect(controllers.refundRequestJourney.routes.BarsLockoutController.barsLockout))
            case None    =>
              Right(
                new BarsVerifiedRequest(
                  request = request,
                  journey = request.journey,
                  sessionId = request.sessionId,
                  numberOfBarsVerifyAttempts = status.attempts
                )
              )
          }
        } recover { case e =>
          logger.error(
            s"[BarsLockoutActionRefiner] failed to retrieve BarsVerifyStatus for nino=${request.journey.nino.toString}, reason:${e.getMessage}"
          )
          Left(InternalServerError)
        }
      case None       =>
        // without a Nino we cannot lookup the BarsVerifyStatus, so just continue
        Future.successful(
          Right(
            new BarsVerifiedRequest(
              request = request,
              journey = request.journey,
              sessionId = request.sessionId
            )
          )
        )
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
