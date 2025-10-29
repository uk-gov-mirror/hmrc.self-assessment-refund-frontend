/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action

import org.apache.pekko.stream.Materializer
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ItSpec
import support.stubbing.{AuditStub, AuthStub}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.{AuthenticatedRequest, PreAuthRequest}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.JourneyTypes.TrackJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps
import uk.gov.hmrc.selfassessmentrefundfrontent.util.CanEqualGivens.sessionIdCanEqual

import scala.concurrent.Future

class AuthorisedSessionRefinerSpec extends ItSpec {

  @SuppressWarnings(Array("org.wartremover.warts.AnyVal"))
  override lazy val configOverrides: Map[String, Any] = Map(
    "auditing.enabled"               -> true,
    "auditing.consumer.baseUri.port" -> wireMockServer.port
  )

  override def fakeAuthConnector: Option[AuthConnector] = None

  implicit val materializer: Materializer = app.injector.instanceOf[Materializer]

  val defaultActionBuilder: DefaultActionBuilder         = app.injector.instanceOf[DefaultActionBuilder]
  val authorisedSessionRefiner: AuthorisedSessionRefiner = app.injector.instanceOf[AuthorisedSessionRefiner]
  val preAuthSessionRefiner: PreAuthSessionRefiner       = app.injector.instanceOf[PreAuthSessionRefiner]

  val authenticatedJourneyAction: ActionBuilder[AuthenticatedRequest, AnyContent] =
    defaultActionBuilder
      .andThen[PreAuthRequest](preAuthSessionRefiner)
      .andThen[AuthenticatedRequest](authorisedSessionRefiner)

  def doTest(request: Request[_], expectedSessionId: SessionId): Future[Result] = authenticatedJourneyAction {
    request =>
      request.sessionId shouldBe expectedSessionId
      Ok
  }(request).run()

  def doTestNoSessionId(request: Request[_]): Future[Result] = authenticatedJourneyAction { _ =>
    Ok
  }(request).run()

  def fakeRequestRefundRequest(didReturnFromIV: Boolean = false): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(
      "GET",
      s"/request-a-self-assessment-refund/refund-amount${if (didReturnFromIV) "?journeyId=1234" else ""}"
    )

  def fakeRequestTrackRefund(didReturnFromIV: Boolean = false): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(
      "GET",
      s"/track-a-self-assessment-refund/refund-request-tracker/start${if (didReturnFromIV) "?journeyId=1234" else ""}"
    )

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", "/self-assessment-refund-frontend/auth/authorise")

  "AuthorisedSessionRefiner" should {
    "allow users to proceed" when {
      "they are Individual with ConfidenceLevel 250 and do not send IdentityVerificationOutcome audit" in {
        stubBackendBusinessJourney()
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)

        val result = doTest(fakeRequest.withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe OK

        AuditStub.verifyNoAuditEvent()
      }
      "they are Organisation with ConfidenceLevel 250 and do not send IdentityVerificationOutcome audit" in {
        stubBackendBusinessJourney()
        AuthStub.authorise(AffinityGroup.Organisation, ConfidenceLevel.L250)

        val result = doTest(fakeRequest.withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe OK

        AuditStub.verifyNoAuditEvent()
      }
      "they are Agent with ConfidenceLevel 50" in {
        stubBackendBusinessJourney()
        AuthStub.authorise(AffinityGroup.Agent, ConfidenceLevel.L50)

        val result = doTest(fakeRequest.withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe OK
      }
    }

    "send IdentityVerificationOutcome on return from IV in refund journey" when {
      "they are Individual" in {
        stubBackendBusinessJourney(nino = Some(Nino("AA111111A")))
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)

        val result =
          doTest(fakeRequestRefundRequest(true).withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe OK

        AuditStub.verifyEventAudited(
          "IdentityVerificationOutcome",
          Json
            .parse(
              s"""{
             |  "isSuccessful": true,
             |  "nino":"AA111111A",
             |  "userType":"Individual"
             |}""".stripMargin
            )
            .as[JsObject]
        )
      }

      "they are Organisation" in {
        stubBackendBusinessJourney(nino = Some(Nino("AA111111A")))
        AuthStub.authorise(AffinityGroup.Organisation, ConfidenceLevel.L250)

        val result =
          doTest(fakeRequestRefundRequest(true).withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe OK

        AuditStub.verifyEventAudited(
          "IdentityVerificationOutcome",
          Json
            .parse(
              s"""{
             |  "isSuccessful": true,
             |  "nino":"AA111111A",
             |  "userType":"Organisation"
             |}""".stripMargin
            )
            .as[JsObject]
        )
      }
    }

    "send IdentityVerificationOutcome on return from IV in tracker journey" when {
      "they are Individual" in {
        stubBackendBusinessJourney(nino = Some(Nino("AA111111A")))
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L250)

        val result = doTest(fakeRequestTrackRefund(true).withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe OK

        AuditStub.verifyEventAudited(
          "IdentityVerificationOutcome",
          Json
            .parse(
              s"""{
             |  "isSuccessful": true,
             |  "nino":"AA111111A",
             |  "userType":"Individual"
             |}""".stripMargin
            )
            .as[JsObject]
        )
      }

      "they are Organisation" in {
        stubBackendBusinessJourney(nino = Some(Nino("AA111111A")))
        AuthStub.authorise(AffinityGroup.Organisation, ConfidenceLevel.L250)

        val result = doTest(fakeRequestTrackRefund(true).withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe OK

        AuditStub.verifyEventAudited(
          "IdentityVerificationOutcome",
          Json
            .parse(
              s"""{
             |  "isSuccessful": true,
             |  "nino":"AA111111A",
             |  "userType":"Organisation"
             |}""".stripMargin
            )
            .as[JsObject]
        )
      }
    }

    "redirect users to IV uplift in refund request journey" when {
      "they have incorrect ConfidenceLevel for Individual" in {
        stubBackendBusinessJourney()
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L50)

        val result = doTest(fakeRequestRefundRequest().withSessionId().withAuthToken(), SessionId("session-deadbeef"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          "http://localhost:9948/iv-stub/uplift?confidenceLevel=250&origin=self-assessment-refund&completionURL=http%3A%2F%2Flocalhost%3A9171%2Frequest-a-self-assessment-refund%2Frefund-amount&failureURL=http%3A%2F%2Flocalhost%3A9171%2Frequest-a-self-assessment-refund%2Fcannot-confirm-identity%3FuserType%3DIndividual"
        )

        AuditStub.verifyEventAudited(
          "RefundAmount",
          Json
            .parse(
              """{"outcome":{"isSuccessful":false,"failureReason":"low confidence level"},"userType":"Individual"}"""
            )
            .as[JsObject]
        )
      }
      "they have incorrect ConfidenceLevel for Organisation" in {
        AuthStub.authorise(AffinityGroup.Organisation, ConfidenceLevel.L50)
        stubBackendBusinessJourney()

        val result = doTest(fakeRequestRefundRequest().withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          "http://localhost:9948/iv-stub/uplift?confidenceLevel=250&origin=self-assessment-refund&completionURL=http%3A%2F%2Flocalhost%3A9171%2Frequest-a-self-assessment-refund%2Frefund-amount&failureURL=http%3A%2F%2Flocalhost%3A9171%2Frequest-a-self-assessment-refund%2Fcannot-confirm-identity%3FuserType%3DOrganisation"
        )

        AuditStub.verifyEventAudited(
          "RefundAmount",
          Json
            .parse(
              """{"outcome":{"isSuccessful":false,"failureReason":"low confidence level"},"userType":"Organisation"}"""
            )
            .as[JsObject]
        )
      }
    }

    "redirect users to IV uplift in refund tracker journey" when {
      "they have incorrect ConfidenceLevel for Individual" in {
        stubBackendBusinessJourney(journeyType = TrackJourney)
        AuthStub.authorise(AffinityGroup.Individual, ConfidenceLevel.L50)

        val result = doTest(fakeRequestTrackRefund().withSessionId().withAuthToken(), SessionId("session-deadbeef"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          "http://localhost:9948/iv-stub/uplift?confidenceLevel=250&origin=self-assessment-refund&completionURL=http%3A%2F%2Flocalhost%3A9171%2Ftrack-a-self-assessment-refund%2Frefund-request-tracker%2Fstart&failureURL=http%3A%2F%2Flocalhost%3A9171%2Ftrack-a-self-assessment-refund%2Fcannot-confirm-identity%3FuserType%3DIndividual"
        )

        AuditStub.verifyEventAudited(
          "ViewRefundStatus",
          Json
            .parse(
              """{"outcome":{"isSuccessful":false,"failureReason":"low confidence level"},"origin":"view and change","userType":"Individual","refunds":[]}"""
            )
            .as[JsObject]
        )
      }
      "they have incorrect ConfidenceLevel for Organisation" in {
        AuthStub.authorise(AffinityGroup.Organisation, ConfidenceLevel.L50)
        stubBackendBusinessJourney(journeyType = TrackJourney)

        val result = doTest(fakeRequestTrackRefund().withSessionId().withAuthToken(), SessionId("session-deadbeef"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          "http://localhost:9948/iv-stub/uplift?confidenceLevel=250&origin=self-assessment-refund&completionURL=http%3A%2F%2Flocalhost%3A9171%2Ftrack-a-self-assessment-refund%2Frefund-request-tracker%2Fstart&failureURL=http%3A%2F%2Flocalhost%3A9171%2Ftrack-a-self-assessment-refund%2Fcannot-confirm-identity%3FuserType%3DOrganisation"
        )

        AuditStub.verifyEventAudited(
          "ViewRefundStatus",
          Json
            .parse(
              """{"outcome":{"isSuccessful":false,"failureReason":"low confidence level"},"origin":"view and change","userType":"Organisation","refunds":[]}"""
            )
            .as[JsObject]
        )
      }
    }

    "redirect users to login" when {
      "no sessionId is found" in {
        stubBackendJourneyNoSessionId()

        val result = doTestNoSessionId(fakeRequest.withAuthToken())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http://localhost:9171/self-assessment-refund/self-assessment-refund/test-only"
        )
      }

      "no bearer token is found" in {
        stubBackendBusinessJourney()

        val result = doTest(fakeRequest.withSession(), SessionId("session-deadbeef"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http://localhost:9171/self-assessment-refund/self-assessment-refund/test-only"
        )
      }
    }
  }

}
