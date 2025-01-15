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

import cats.syntax.eq.catsSyntaxEq
import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.selfassessmentrefundfrontend.config.AppConfig
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.BarsLockout

@Singleton
class BarsLockoutController @Inject() (
    barsLockoutView: BarsLockout,
    actions:         Actions,
    mcc:             MessagesControllerComponents
)(implicit config: AppConfig, languageUtils: LanguageUtils) extends FrontendController(mcc) with I18nSupport {

  //checking for a return url, returning urls when none is found
  override def messagesApi: MessagesApi = mcc.messagesApi
  val barsLockout: Action[AnyContent] = actions.barsLockedOutJourneyAction { implicit request =>
    val returnUrl = request.returnUrl match {
      case Some(url) => url.value
      case None =>
        if (request.request.affinityGroup.toString === AffinityGroup.Agent.toString) {
          config.viewAndChangeHubAgentUrl
        } else {
          config.viewAndChangeHubIndividualOrOrganisationUrl
        }
    }

    Ok(barsLockoutView(request.barsLockoutExpiryTime, returnUrl, request.isAgent))
  }

}
