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

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.selfassessmentrefundfrontend.config.ErrorHandler
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.RefundProcessingPageModel
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService
import uk.gov.hmrc.selfassessmentrefundfrontend.util.{AmountFormatter, ApplicationLogging}
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.trackRefundJourney.RefundProcessingPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RefundProcessingController @Inject() (
    actions:              Actions,
    mcc:                  MessagesControllerComponents,
    refundProcessingPage: RefundProcessingPage,
    languageUtils:        LanguageUtils,
    repaymentsService:    RepaymentsService,
    errorHandler:         ErrorHandler
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with ApplicationLogging {

  def onPageLoad(number: RequestNumber): Action[AnyContent] = actions.authenticatedTrackJourneyAction.async { implicit request =>
    repaymentsService
      .repayment(request.journey.nino.getOrElse(throw new Throwable("nino not found")), number)
      .map { repayment =>
        val claim = repayment.claim
        Ok(refundProcessingPage(RefundProcessingPageModel(
          amount       = AmountFormatter.formatAmount(claim.amount),
          reference    = claim.key,
          requestDate  = languageUtils.Dates.formatDate(claim.created),
          refundByDate = languageUtils.Dates.formatDate(claim.created.plusDays(38))
        )))
      }.recoverWith {
        case e: Throwable =>
          logger.error(s"[RefundProcessingController][onPageLoad] Error during page load: ${request.journey.toLogMessage}", e)
          errorHandler.internalServerErrorTemplate
            .map(InternalServerError(_))
      }
  }
}
