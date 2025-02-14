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

import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{mapping, number}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.config.AppConfig
import uk.gov.hmrc.selfassessmentrefundfrontend.testonly.controllers.StartJourneyController.origin
import uk.gov.hmrc.selfassessmentrefundfrontend.testonly.model.{StartJourneyOptions, StartJourneyPageModel, StartJourneyPresets}
import uk.gov.hmrc.selfassessmentrefundfrontend.testonly.services.StartJourneyService
import uk.gov.hmrc.selfassessmentrefundfrontend.testonly.views.html.StartJourneyPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartJourneyController @Inject() (
    val authConnector: AuthConnector,
    appConfig:         AppConfig,
    page:              StartJourneyPage,
    service:           StartJourneyService,
    mcc:               MessagesControllerComponents
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with AuthorisedFunctions {

  private val logger = Logger(this.getClass)

  def showStartJourneyPage(
      options: StartJourneyOptions
  ): Action[AnyContent] = Action.async { implicit request =>
    val form = StartJourneyOptions.form
      .fill(options)

    val pageModel = StartJourneyPageModel(
      form,
      StartJourneyPresets.presets
    )

    Future.successful(Ok(page(pageModel)))
  }

  val submitStartJourneyForm: Action[AnyContent] = Action.async { implicit request =>
    val form = StartJourneyOptions.form.bindFromRequest()

    form.value match {
      case Some(startOptions) =>
        authorised() {
          service.start(startOptions, origin)
        }.recover {
          case _: NoActiveSession =>
            val continueUrl = Seq(
              appConfig.frontendBaseUrl + uk.gov.hmrc.selfassessmentrefundfrontend.testonly.controllers.routes.StartJourneyController.showStartJourneyPage(startOptions).url
            )
            Redirect(appConfig.loginUrl, Map("continue" -> continueUrl))
          case e: AuthorisationException =>
            logger.warn(s"Unauthorised because of ${e.reason}")
            BadRequest("ERROR" + e.reason)
        }
      case None =>
        val pageModel = StartJourneyPageModel(form)

        Future.successful(Ok(page(pageModel)))
    }
  }

  val selectPreset: Action[AnyContent] = Action.async { implicit request =>
    val index = Form(
      mapping(
        "index" -> number
      )(identity)(Some(_))
    ).bindFromRequest().get

    val details = StartJourneyPresets.fromIndex(index)

    Future.successful(Redirect(routes.StartJourneyController.showStartJourneyPage(details)))
  }

  val redirectToStartJourneyPage: Action[AnyContent] = Action {
    val details = StartJourneyOptions.default

    Redirect(uk.gov.hmrc.selfassessmentrefundfrontend.testonly.controllers.routes.StartJourneyController.showStartJourneyPage(details))
  }

}

object StartJourneyController {
  val origin = "sar-frontend-testonly"
}
