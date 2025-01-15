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

package uk.gov.hmrc.selfassessmentrefundfrontend.connectors

import com.google.inject.{Inject, Singleton}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.selfassessmentrefundfrontend.config.AppConfig
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.PreAuthRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.JourneyTypes

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.Future

@Singleton
class IVUpliftConnector @Inject() (appConfig: AppConfig) {

  def performUplift[A](request: PreAuthRequest[A], userType: String): Future[Result] = {
    val journeyTypeBasedFailedUrl = request.journey.journeyType match {
      case JourneyTypes.RefundJourney => controllers.refundRequestJourney.routes.WeCannotConfirmYourIdentityController.failedUplift(userType = userType).url
      case JourneyTypes.TrackJourney  => controllers.trackRefundJourney.routes.WeCannotConfirmYourIdentityController.failedUplift(userType = userType).url
    }

    val failedURL = URLEncoder.encode(appConfig.ivCallbackUrl + journeyTypeBasedFailedUrl, StandardCharsets.UTF_8)

    val continueURL = URLEncoder.encode(appConfig.ivCallbackUrl + request.path, StandardCharsets.UTF_8)

    val confidenceLevel = appConfig.confidenceLevel

    val upliftBase = appConfig.upliftUrl
    val upliftOrigin = appConfig.upliftOrigin

    val url = s"$upliftBase?confidenceLevel=$confidenceLevel&origin=$upliftOrigin&completionURL=$continueURL&failureURL=$failedURL"
    Future.successful(Redirect(url))
  }

}
