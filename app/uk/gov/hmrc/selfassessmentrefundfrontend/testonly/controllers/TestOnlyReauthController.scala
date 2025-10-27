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

package uk.gov.hmrc.selfassessmentrefundfrontend.testonly.controllers

import com.google.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.DummyReauth

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyReauthController @Inject() (
  i18nSupport:       I18nSupport,
  val authConnector: AuthConnector,
  val dummyReauth:   DummyReauth,
  mcc:               MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with AuthorisedFunctions {

  import i18nSupport._

  def localAuth(continueUrl: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      Future.successful(Ok(dummyReauth(continueUrl)))
    }
  }
}
