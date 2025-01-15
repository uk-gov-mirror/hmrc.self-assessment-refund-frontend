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

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.AuditService
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.{JourneyConnector, RepaymentsConnector}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.Journey
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.{RepaymentCreatedResponse, RequestNumber}
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.YouNeedToSignInAgainPage
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YouNeedToSignInAgainController @Inject() (
    i18n:                     I18nSupport,
    youNeedToSignInAgainPage: YouNeedToSignInAgainPage,
    val authConnector:        AuthConnector,
    auditService:             AuditService,
    repaymentsConnector:      RepaymentsConnector,
    journeyConnector:         JourneyConnector,
    actions:                  Actions,
    mcc:                      MessagesControllerComponents
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with AuthorisedFunctions {

  import i18n._

  private val logger = Logger(this.getClass)

  val onPageLoad: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>
    Future.successful(Ok(youNeedToSignInAgainPage(request.isAgent)))
  }

  val onSubmit: Action[AnyContent] = actions.authenticatedRefundJourneyAction {
    Redirect(refundRequestJourney.routes.ReauthController.reauthentication)
  }

  val reauthSuccessful: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>

    val journey = request.journey
    val hasStartedReauth = journey.hasStartedReauth
    val headerData = new JsObject(request.headers.toMap.map(
      x => x._1 -> JsString(x._2 mkString ",")
    ))

    hasStartedReauth match {
      case Some(true) =>
        (for {
          createRepaymentResponse <- repaymentsConnector.createRepayment(journey.id, headerData)
          updatedJourney <- journeyConnector.findLatestBySessionId()
        } yield (createRepaymentResponse, updatedJourney)).map {
          case (Right(RepaymentCreatedResponse(number, maybeNrsSubmissionId)), journey) =>
            auditAndRedirect(Some(number), journey, maybeNrsSubmissionId, Some(request.affinityGroup.toString))
          case (Left(upstreamError), journey) =>
            logger.warn(s"Failed to create repayment. Error: ${upstreamError.getMessage}")
            auditAndRedirect(None, journey, None, Some(request.affinityGroup.toString))
        } recover {
          case e =>
            //create repayment already handles its upstream errors and so this recover should only trigger for the journey connector
            logger.warn(s"[YouNeedToSignInAgainController][reauthSuccessful] Failed to retrieve journey. Error: ${e.getMessage}")
            auditAndRedirect(None, journey, None, Some(request.affinityGroup.toString))
        }
      case _ =>
        logger.warn(s"[YouNeedToSignInAgainController][reauthSuccessful] Reauth callback endpoint called when hasStartedReauth=${hasStartedReauth.toString}")
        Future.successful(auditAndRedirect(None, journey, None, Some(request.affinityGroup.toString)))
    }
  }

  private def auditAndRedirect(
      requestNumber:    Option[RequestNumber],
      journey:          Journey,
      optSubmissionId:  Option[String],
      optAffinityGroup: Option[String]
  )(implicit hc: HeaderCarrier) = {
    requestNumber match {
      case Some(number) =>
        auditService.auditRefundRequestEvent(journey, optSubmissionId, optAffinityGroup)
        Redirect(refundRequestJourney.routes.RepaymentConfirmationController.confirmation(number))
      case None =>
        auditService.auditRefundRequestEvent(journey, None, optAffinityGroup)
        Redirect(refundRequestJourney.routes.YourRefundRequestNotSubmittedController.show)
    }
  }

}

