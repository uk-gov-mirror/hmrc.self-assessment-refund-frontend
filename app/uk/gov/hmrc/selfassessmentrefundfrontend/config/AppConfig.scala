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

package uk.gov.hmrc.selfassessmentrefundfrontend.config

import play.api.Configuration
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney.routes

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  private lazy val selfBaseUrl: String = config.get[String]("frontend-base-url")

  private def selfUrl(call: Call): String = s"$selfBaseUrl${call.url}"

  lazy val refundTrackerUrl: String = {
    val trackerUrl = routes.RefundTrackerController.refundTracker
    selfUrl(trackerUrl)
  }

  val welshLanguageSupportEnabled: Boolean =
    config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  val exampleExternalUrl: String                = servicesConfig.baseUrl("bank-account-verification-callback.external")
  val itsaUrl: String                           = servicesConfig.baseUrl("itsa-viewer")
  val selfAssessmentRepaymentBackendUrl: String = servicesConfig.baseUrl("self-assessment-refund-backend")
  val selfAssessmentRefundStubsUrl: String      = servicesConfig.baseUrl("self-assessment-refund-stubs")
  val reauthenticationUrl: String               =
    servicesConfig.getConfString("reauthentication.path", throwConfigNotFoundError("reauthentication.path"))

  private val authLoginStubPath: String = servicesConfig.getConfString("auth-login-stub.path", "")
  val authLoginStubUrl: String          = servicesConfig.baseUrl("auth-login-stub") +
    authLoginStubPath + "?continue=" +
    exampleExternalUrl + "/self-assessment-refund" +
    uk.gov.hmrc.selfassessmentrefundfrontend.testonly.controllers.routes.StartJourneyController.redirectToStartJourneyPage.url

  val authTimeoutSeconds: Int          = config.get[FiniteDuration]("timeout.inactivity-timeout").toSeconds.toInt
  val authTimeoutCountdownSeconds: Int =
    config.get[FiniteDuration]("timeout.inactivity-countdown").toSeconds.toInt
  val frontendBaseUrl: String          = servicesConfig.getString(s"frontend-base-url")
  val ivCallbackUrl: String            = servicesConfig.getString(s"iv-callback-base-url")
  val upliftUrl: String                = servicesConfig.baseUrl("iv-uplift") +
    servicesConfig.getConfString("iv-uplift.path", throwConfigNotFoundError("iv-uplift.path"))
  val upliftOrigin: String             =
    servicesConfig.getConfString("iv-uplift.origin", throwConfigNotFoundError("iv-uplift.origin"))
  val confidenceLevel: String          =
    servicesConfig.getConfString("iv-uplift.confidenceLevel", throwConfigNotFoundError("iv-uplift.confidenceLevel"))
  val loginUrl: String                 = servicesConfig.getString("urls.login")

  val logoutUrl: String =
    servicesConfig.getString("urls.logout") +
      "?continue=" +
      servicesConfig.getString("urls.logoutContinue")

  val creditAndRefundsUrl: String                         = itsaUrl + servicesConfig.getString("navigation.paths.creditAndRefunds")
  val creditAndRefundsAgentsUrl: String                   = itsaUrl + servicesConfig.getString("navigation.paths.creditAndRefundsAgents")
  val refundIssuedAgentUrl: String                        = itsaUrl + servicesConfig.getString("navigation.paths.refundIssuedAgent")
  val refundIssuedIndividualOrOrganisationUrl: String     =
    itsaUrl + servicesConfig.getString("navigation.paths.refundIssuedIndividualOrOrganisation")
  val viewAndChangeHubIndividualOrOrganisationUrl: String =
    itsaUrl + servicesConfig.getString("navigation.paths.viewAndChangeHubIndividualOrOrganisation")
  val viewAndChangeHubAgentUrl: String                    = itsaUrl + servicesConfig.getString("navigation.paths.viewAndChangeHubAgent")

  val barsBaseUrl: String = servicesConfig.baseUrl("bank-account-reputation")

  lazy val feedbackFrontendUrl: String = servicesConfig.getString("urls.feedback-frontend")
  lazy val feedbackUrl                 = s"$feedbackFrontendUrl/feedback/self-assessment-refund"

  private def throwConfigNotFoundError(key: String): Nothing =
    throw new RuntimeException(s"Could not find config key '$key'")

}
