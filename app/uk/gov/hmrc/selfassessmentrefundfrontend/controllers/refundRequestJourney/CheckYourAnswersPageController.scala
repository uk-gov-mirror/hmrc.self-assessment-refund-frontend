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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.JourneyConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.CheckYourAnswersPage
import uk.gov.hmrc.selfassessmentrefundfrontend.views.refundrequestjourney.CheckYourAnswersHelper

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersPageController @Inject() (
    i18n:                   I18nSupport,
    mcc:                    MessagesControllerComponents,
    checkYourAnswersPage:   CheckYourAnswersPage,
    actions:                Actions,
    journeyConnector:       JourneyConnector,
    checkYourAnswersHelper: CheckYourAnswersHelper
)(implicit ec: ExecutionContext) extends FrontendController(mcc) {

  import i18n._

  val start: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>
    val journey = request.journey
    val amount = journey.amount.getOrElse(sys.error("could not find amount"))
    val bankAccountInfo = journey.bankAccountInfo.getOrElse(sys.error("Could not find bank details"))
    val html = checkYourAnswersPage(
      summaryList = checkYourAnswersHelper.buildSummaryList(amount, journey.accountType.getOrElse(sys.error("account type not found")), bankAccountInfo),
      formAction  = routes.CheckYourAnswersPageController.confirm,
      isAgent     = request.isAgent
    )

    journeyConnector.setJourney(journey.id, journey.copy(nrsWebpage = Some(html.toString()))).map { _ =>
      Ok(html).removingFromSession("self-assessment-refund.changing-amount-from-cya-page")
    }
  }

  val confirm: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { _ =>
    Future.successful(Redirect(refundRequestJourney.routes.YouNeedToSignInAgainController.onPageLoad))
  }

  val changeAmount: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request: Request[_] =>
    Future.successful(
      Redirect(uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.SelectRepaymentAmountController.selectAmount)
        .addingToSession("self-assessment-refund.changing-amount-from-cya-page" -> "redirectToCYA")
    )
  }

  val changeAccount: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request: Request[_] =>
    Future.successful(
      Redirect(uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.AccountTypeController.getAccountType)
        .addingToSession("self-assessment-refund.changing-account-from-cya-page" -> "redirectToCYA")
    )
  }

}
