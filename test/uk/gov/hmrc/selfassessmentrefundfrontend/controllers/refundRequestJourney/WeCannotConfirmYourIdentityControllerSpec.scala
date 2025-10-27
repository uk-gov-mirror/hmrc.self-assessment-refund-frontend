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

import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Cookie
import play.api.test.{FakeRequest, Helpers}
import support.ItSpec
import support.stubbing.AuditStub
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.{WeCannotConfirmYourIdentityController => WeCannotConfirmYourIdentityControllerRefundRequest}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney.{WeCannotConfirmYourIdentityController => WeCannotConfirmYourIdentityControllerRefundTracker}
import uk.gov.hmrc.selfassessmentrefundfrontend.pages.WeCannotConfirmYourIdentityPageTesting

class WeCannotConfirmYourIdentityControllerSpec extends ItSpec with WeCannotConfirmYourIdentityPageTesting {

  @SuppressWarnings(Array("org.wartremover.warts.AnyVal"))
  override lazy val configOverrides: Map[String, Any] = Map(
    "auditing.enabled"               -> true,
    "auditing.consumer.baseUri.port" -> wireMockServer.port
  )

  "a failedUplift" should {
    "display WeCannotConfirmYourIdentity page in refund request journey" in {
      val upLift: WeCannotConfirmYourIdentityControllerRefundRequest =
        fakeApplication().injector.instanceOf[WeCannotConfirmYourIdentityControllerRefundRequest]

      val request = FakeRequest(Helpers.POST, "")

      val response = upLift.failedUplift(userType = "Individual")(request)

      response.checkPageIsDisplayed(
        expectedHeading = "We cannot confirm your identity",
        expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
        contentChecks = checkPageContent,
        expectedStatus = Status.OK,
        withBackButton = false,
        journey = "request"
      )

      AuditStub.verifyEventAudited(
        "IdentityVerificationOutcome",
        Json.parse("""{"isSuccessful":false,"userType":"Individual"}""").as[JsObject]
      )
    }

    "display welsh WeCannotConfirmYourIdentity page in refund request journey" in {
      val upLift: WeCannotConfirmYourIdentityControllerRefundRequest =
        fakeApplication().injector.instanceOf[WeCannotConfirmYourIdentityControllerRefundRequest]

      val request = FakeRequest(Helpers.POST, "").withCookies(Cookie("PLAY_LANG", "cy"))

      val response = upLift.failedUplift(userType = "Individual")(request)

      response.checkPageIsDisplayed(
        expectedHeading = "Ni allwn gadarnhau pwy ydych",
        expectedServiceLink = "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
        contentChecks = checkPageContentWelsh,
        expectedStatus = Status.OK,
        withBackButton = false,
        journey = "request",
        welsh = true
      )

      AuditStub.verifyEventAudited(
        "IdentityVerificationOutcome",
        Json.parse("""{"isSuccessful":false,"userType":"Individual"}""").as[JsObject]
      )
    }

    "display WeCannotConfirmYourIdentity page in refund tracker journey" in {
      val upLift: WeCannotConfirmYourIdentityControllerRefundTracker =
        fakeApplication().injector.instanceOf[WeCannotConfirmYourIdentityControllerRefundTracker]

      val request = FakeRequest(Helpers.POST, "")

      val response = upLift.failedUplift(userType = "Individual")(request)

      response.checkPageIsDisplayed(
        expectedHeading = "We cannot confirm your identity",
        expectedServiceLink = "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker",
        contentChecks = checkPageContent,
        expectedStatus = Status.OK,
        withBackButton = false,
        journey = "track"
      )

      AuditStub.verifyEventAudited(
        "IdentityVerificationOutcome",
        Json.parse("""{"isSuccessful":false,"userType":"Individual"}""").as[JsObject]
      )
    }
  }
}
