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

import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.config.ErrorHandler
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RepaymentStatusController @Inject() (
    actions:           Actions,
    mcc:               MessagesControllerComponents,
    repaymentsService: RepaymentsService,
    errorHandler:      ErrorHandler
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with Logging {

  def statusOf(number: RequestNumber): Action[AnyContent] = actions.authenticatedTrackJourneyAction.async { implicit request =>

    repaymentsService.repayment(request.journey.nino.getOrElse(throw new Throwable("nino not found")), number)
      .map {
        case atp: ApprovedTaxRepayment                     => Redirect(trackRefundJourney.routes.RefundApprovedController.showApprovedPage(atp.claim.key))
        case processing: ProcessingTaxRepayment            => Redirect(trackRefundJourney.routes.RefundProcessingController.onPageLoad(processing.claim.key))
        case processRisking: ProcessingRiskingTaxRepayment => Redirect(trackRefundJourney.routes.RefundProcessingController.onPageLoad(processRisking.claim.key))
        case rejected: RejectedTaxRepayment                => Redirect(trackRefundJourney.routes.RefundRejectedController.onPageLoad(rejected.claim.key))
      }.recoverWith {
        case e: Exception =>
          logger.warn(s"[RepaymentStatusController][statusOf] - Unsuccessful retrieval from the repayments service", e)
          errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
      }
  }
}

