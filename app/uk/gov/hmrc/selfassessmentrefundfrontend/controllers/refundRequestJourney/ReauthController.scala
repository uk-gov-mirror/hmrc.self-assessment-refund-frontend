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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.config.AppConfig
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.JourneyConnector

import scala.concurrent.ExecutionContext

@Singleton
class ReauthController @Inject() (
    appConfig:         AppConfig,
    actions:           Actions,
    journeyConnector:  JourneyConnector,
    val authConnector: AuthConnector,
    mcc:               MessagesControllerComponents
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with AuthorisedFunctions {

  val reauthentication: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>
    val continue = refundRequestJourney.routes.YouNeedToSignInAgainController.reauthSuccessful.url
    val journeyId = request.journey.id

    val base = appConfig.reauthenticationUrl
    val url = s"$base/reauthentication?continue=$continue"
    journeyConnector.setJourney(journeyId, request.journey.copy(hasStartedReauth = Some(true)))
      .map(_ => Redirect(url))
  }

}
