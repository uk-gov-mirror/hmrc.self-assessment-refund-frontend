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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.services.{RefundTrackerViewHelper, RepaymentsService}
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.trackRefundJourney.RefundTrackerPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RefundTrackerController @Inject() (
    actions:                Actions,
    mcc:                    MessagesControllerComponents,
    appConfig:              AppConfig,
    refundTrackerPage:      RefundTrackerPage,
    repaymentService:       RepaymentsService,
    errorHandler:           ErrorHandler,
    refundTackerViewHelper: RefundTrackerViewHelper
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with Logging {

  val startRefundTracker: Action[AnyContent] = actions.authenticatedTrackJourneyAction {
    Redirect(trackRefundJourney.routes.RefundTrackerController.refundTracker)
  }

  val refundTracker: Action[AnyContent] = actions.authenticatedTrackJourneyAction.async { implicit request =>
    val creditAndRefundsUrl = if (request.isAgent) appConfig.creditAndRefundsAgentsUrl else appConfig.creditAndRefundsUrl
    repaymentService.repayments(request.journey.nino.getOrElse(sys.error("nino not found"))).map(taxRepayments => {
      val yearlyRefundsModel = refundTackerViewHelper.refundTrackerYearModelMap(taxRepayments)
      Ok(refundTrackerPage(yearlyRefundsModel, creditAndRefundsUrl))
    }).recoverWith {
      case e: Exception =>
        logger.warn(s"[RefundTrackerController][refundTracker] - Unsuccessful retrieval from the repayments service", e)
        errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
    }
  }
}
