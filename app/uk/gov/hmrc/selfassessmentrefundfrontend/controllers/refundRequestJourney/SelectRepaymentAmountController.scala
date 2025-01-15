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

import cats.syntax.eq._
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.AuditService
import uk.gov.hmrc.selfassessmentrefundfrontend.config.ErrorHandler
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.JourneyConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.BarsVerifiedRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.PaymentMethod.{BACS, Card, PaymentOrder}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.SelectAmountChoice.{Full, Partial, Suggested}
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.SelectAmountPageModel
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.SelectAmountPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

@Singleton
class SelectRepaymentAmountController @Inject() (
    actions:          Actions,
    errorHandler:     ErrorHandler,
    i18n:             I18nSupport,
    journeyConnector: JourneyConnector,
    mcc:              MessagesControllerComponents,
    selectAmountPage: SelectAmountPage,
    auditService:     AuditService
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with Logging {

  import i18n._

  val selectAmount: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>
    val journey = request.journey

    journey.amount match {
      case Some(amount) =>
        amount match {
          case Amount(fullFromVC, repayment, partialRepaymentSelected, ac @ Some(availableCredit), Some(allocatedAmount), suggestedRepaymentSelected) =>
            val model = SelectAmountPageModel(repayment, partialRepaymentSelected, availableCredit, suggestedAmount(allocatedAmount, availableCredit), suggestedRepaymentSelected, request.isAgent)

              def isAmountMatching: Boolean = (ac, fullFromVC) match {
                case (Some(avAmount), Some(vcAmount)) if avAmount === vcAmount => true
                case (Some(_), None) => true
                case _ => false
              }

            if (isAmountMatching) {
              Future.successful(Ok(selectAmountPage(model)))
            } else {
              auditService.auditRefundAmount(
                balanceDueWithin30Days = Some(allocatedAmount),
                amountAvailable        = Some(availableCredit),
                amountChosen           = None,
                affinityGroup          = Some(request.affinityGroup),
                maybeNino              = journey.nino,
                failureReason          = Some("availableCredit does not match the value sent from view and change")
              )
              logAndReturnErrorPage(method = "selectAmount", s"[SelectRepaymentAmountController][selectAmount] Amounts from V&C and API#1553 are not the same, '${fullFromVC.map(_.toString()).getOrElse("missing")}' does not match '${availableCredit.toString}'")
            }
          case _ =>
            auditService.auditRefundAmount(
              balanceDueWithin30Days = amount.balanceDueWithin30Days,
              amountAvailable        = amount.availableCredit,
              amountChosen           = None,
              affinityGroup          = Some(request.affinityGroup),
              maybeNino              = journey.nino,
              failureReason          = Some("Amount object in journey missing required attributes")
            )
            logAndReturnErrorPage(method = "selectAmount")
        }
      case _ =>
        auditService.auditRefundAmount(
          balanceDueWithin30Days = None,
          amountAvailable        = None,
          amountChosen           = None,
          affinityGroup          = Some(request.affinityGroup),
          maybeNino              = journey.nino,
          failureReason          = Some("Amount object in journey is None")
        )
        logAndReturnErrorPage(method = "selectAmount")
    }
  }

  val submitAmount: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>
    val journey = request.journey
    val redirectToCYA: Option[String] = request.session.get("self-assessment-refund.changing-amount-from-cya-page")

    journey.amount match {
      case Some(amount) =>
        amount match {
          case Amount(_, _, _, Some(availableCredit), Some(balanceDueWithin30Days), _) =>
            val model = SelectAmountPageModel(availableCredit = availableCredit, suggestedAmount = suggestedAmount(balanceDueWithin30Days, availableCredit), isAgent = request.isAgent).withFormBound()
            val formData = model.form.data
              def auditRefundAmount(amountChosen: Option[BigDecimal]): Unit = {
                auditService.auditRefundAmount(
                  balanceDueWithin30Days = Some(balanceDueWithin30Days),
                  amountAvailable        = Some(availableCredit),
                  amountChosen           = amountChosen,
                  affinityGroup          = Some(request.affinityGroup),
                  maybeNino              = journey.nino
                )
              }

              def storeChosenAmount(): Future[Unit] = formData.get("choice").map(SelectAmountChoice.withNameLowercaseOnly) match {
                case Some(Suggested) =>
                  auditRefundAmount(amount.suggestedAmount)
                  journeyConnector.setJourney(journey.id, journey.copy(amount = Some(amount.setSuggestedRepayment(amount.suggestedAmount))))
                case Some(Full) =>
                  auditRefundAmount(amount.availableCredit)
                  journeyConnector.setJourney(journey.id, journey.copy(amount = Some(amount.setFullRepayment(amount.availableCredit))))
                case Some(Partial) =>
                  val partialAmount = Some(BigDecimal(AmountFormatter.sanitize(formData.get("amount"))))
                  auditRefundAmount(partialAmount)
                  journeyConnector.setJourney(journey.id, journey.copy(amount = Some(amount.setPartialRepayment(partialAmount))))
                case _ => sys.error(s"[SelectRepaymentAmountController][storeChosenAmount] Data not found in the submitted form.")
              }

            if (model.form.hasErrors) {
              Future.successful(Ok(selectAmountPage(model)))
            } else {
              storeChosenAmount()
                .flatMap(_ => handleRedirect(redirectToCYA)(request))
            }
          case _ =>
            logAndReturnErrorPage(method = "submitAmount")
        }
      case _ =>
        logAndReturnErrorPage(method = "submitAmount")
    }
  }

  private def suggestedAmount(balanceDueWithin30Days: BigDecimal, availableCredit: BigDecimal): Option[BigDecimal] =
    if (availableCredit <= balanceDueWithin30Days) {
      None
    } else {
      Some(availableCredit - balanceDueWithin30Days).map(_.setScale(2, RoundingMode.DOWN))
    }

  private def handleRedirect(redirectToCYA: Option[String])(implicit request: BarsVerifiedRequest[_]): Future[Result] = {
    (request.journey.paymentMethod, redirectToCYA) match {
      case (_, Some("redirectToCYA")) => Future.successful(Redirect(controllers.refundRequestJourney.routes.CheckYourAnswersPageController.start))
      case (Some(value), _)           => Future(handlePaymentMethod(value))
      case (None, _) => journeyConnector.lastPaymentMethod(request.journey.id).map { method =>
        handlePaymentMethod(method)
      } recover {
        case e =>
          logger.error(s"[SelectRepaymentAmountController][handleRedirect] Failed to retrieve payment method. Error: ${e.getMessage}")
          sys.error("[SelectRepaymentAmountController][handleRedirect]  Failed to retrieve payment method.")
      }
    }
  }

  private def handlePaymentMethod(paymentMethod: PaymentMethod)(implicit request: BarsVerifiedRequest[_]): Result = {
    request.affinityGroup match {
      case Individual | Organisation => individualAndOrgRedirects(paymentMethod)
      case Agent                     => agentRedirects(paymentMethod)
      case _                         => sys.error("[SelectRepaymentAmountController][handlePaymentMethod] Unsupported affinity group.")
    }
  }

  private def agentRedirects(paymentMethod: PaymentMethod): Result = {
    paymentMethod match {
      case Card                => Redirect(controllers.refundRequestJourney.routes.HowYouWillGetYourRefundController.onPageLoadAgent)
      case BACS | PaymentOrder => Redirect(controllers.refundRequestJourney.routes.WeNeedBankDetailsController.onPageLoadAgent)
      case _                   => sys.error("[SelectRepaymentAmountController][agentRedirects] Unsupported payment method.")
    }
  }

  private def individualAndOrgRedirects(paymentMethod: PaymentMethod): Result = {
    paymentMethod match {
      case Card                => Redirect(controllers.refundRequestJourney.routes.HowYouWillGetYourRefundController.onPageLoad)
      case BACS | PaymentOrder => Redirect(controllers.refundRequestJourney.routes.WeNeedBankDetailsController.onPageLoad)
      case _                   => sys.error("[SelectRepaymentAmountController][individualAndOrgRedirects] Unsupported payment method.")
    }
  }

  private def logAndReturnErrorPage(method: String, message: String = "Not enough data in mongo to proceed.")(implicit request: BarsVerifiedRequest[_]): Future[Result] = {
    logger.error(s"[SelectRepaymentAmountController][$method] $message ${request.journey.toLogMessage}")
    errorHandler.standardErrorTemplate(
      Messages("global.error.InternalServerError500.title"),
      Messages("global.error.InternalServerError500.heading"),
      Messages("global.error.InternalServerError500.message")
    )
      .map(InternalServerError(_))
  }
}
