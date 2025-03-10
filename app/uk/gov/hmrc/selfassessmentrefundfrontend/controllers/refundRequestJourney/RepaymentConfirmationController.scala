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

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.selfassessmentrefundfrontend.config.AppConfig
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.RepaymentsConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod.Card
import uk.gov.hmrc.selfassessmentrefundfrontend.model.SaUtr
import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.RefundConfirmationPageModel
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.RepaymentConfirmationPage

import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepaymentConfirmationController @Inject() (
    i18n:                      I18nSupport,
    actions:                   Actions,
    mcc:                       MessagesControllerComponents,
    languageUtils:             LanguageUtils,
    repaymentsConnector:       RepaymentsConnector,
    repaymentConfirmationPage: RepaymentConfirmationPage
)(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc) with Logging {

  import i18n._

  private val REFUND_PROCESSING_DAYS = 38

  def confirmation(@unused requestNumber: RequestNumber): Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>
    val futureSaUtr: Future[SaUtr] = if (request.isAgent) {
      repaymentsConnector.getSaUtr(request.journey.nino.getOrElse(sys.error("Could not find nino")))
    } else {
      Future.successful(SaUtr(None))
    }

    futureSaUtr.map{ saUtr =>
      val amount = request.journey.amount.getOrElse(sys.error("Could not find amount"))
      val refundConfirmation = request.journey.repaymentConfirmation.getOrElse(sys.error("Could not find refund confirmation"))
      val isLastPaymentByCard = request.journey.paymentMethod.contains(Card)
      val bankAccountEndingDigits = request.journey.bankAccountInfo.getOrElse(sys.error("Bank account info missing")).accountNumber.value.takeRight(3)
      val bankAccountName = request.journey.bankAccountInfo.getOrElse(sys.error("Bank account name missing")).name
      val model = RefundConfirmationPageModel(
        refundConfirmation.repaymentRequestNumber,
        languageUtils.Dates.formatDate(refundConfirmation.processingDate.toLocalDate),
        AmountFormatter.formatAmount(amount.repay),
        languageUtils.Dates.formatDate(refundConfirmation.processingDate.plusDays(REFUND_PROCESSING_DAYS).toLocalDate),
        bankAccountEndingDigits,
        bankAccountName,
        isLastPaymentByCard,
        request.isAgent,
        saUtr
      )

      Ok(repaymentConfirmationPage(model))
    }
  }
}
