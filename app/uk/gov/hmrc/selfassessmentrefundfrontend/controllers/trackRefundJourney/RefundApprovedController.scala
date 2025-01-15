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

import javax.inject.Inject
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.selfassessmentrefundfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.AuthenticatedRequest.request2Messages
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService.ApprovedTaxRepayment
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.trackRefundJourney.RefundApprovedPage

import scala.concurrent.ExecutionContext

class RefundApprovedController @Inject() (
    mcc:                MessagesControllerComponents,
    actions:            Actions,
    languageUtils:      LanguageUtils,
    appConfig:          AppConfig,
    refundApprovedPage: RefundApprovedPage,
    repaymentsService:  RepaymentsService,
    errorHandler:       ErrorHandler
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with Logging {

  def showApprovedPage(requestNumber: RequestNumber): Action[AnyContent] = actions.authenticatedTrackJourneyAction.async { implicit request =>
    repaymentsService
      .repayment(request.journey.nino.getOrElse(throw new Throwable("nino not found")), requestNumber)
      .map {
        case repayment: ApprovedTaxRepayment =>
          lazy val redirectUrl = if (request.isAgent) appConfig.refundIssuedAgentUrl else appConfig.refundIssuedIndividualOrOrganisationUrl

          Ok(refundApprovedPage(
            amount          = AmountFormatter.formatAmount(repayment.claim.amount),
            completedDate   = languageUtils.Dates.formatDate(repayment.completed),
            refundReference = repayment.claim.key.value,
            moreDetailsUrl  = s"$redirectUrl/${repayment.claim.key.value}"
          ))
        case e =>
          logger.warn(s"[RepaymentApprovedController][showApprovedPage] - ApprovedTaxRepayment not found in Journey, found ${e.toString} instead")
          InternalServerError("")
      }.recoverWith {
        case e: Throwable =>
          logger.warn(s"[RepaymentApprovedController][showApprovedPage] - Unsuccessful retrieval from the repayments service", e)
          errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
      }
  }
}
