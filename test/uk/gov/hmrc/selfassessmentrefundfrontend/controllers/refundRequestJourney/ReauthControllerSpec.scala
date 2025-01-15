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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.ItSpec
import uk.gov.hmrc.http.{SessionId, SessionKeys}
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.AuditFlags
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{Journey, JourneyId, JourneyTypes}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll

import java.time.OffsetDateTime

class ReauthControllerSpec extends ItSpec {

  val controller: ReauthController = fakeApplication().injector.instanceOf[ReauthController]

  trait JourneyFixture {
    val sessionId: SessionId = SessionId(TdAll.sessionId)
    val journeyId: JourneyId = TdAll.journeyId
    val nino: Nino = Nino("AA111111A")
  }

  trait RequestWithSessionFixture extends JourneyFixture {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.sessionId -> sessionId.value)
  }

  "the Reauth controller" when {
    "called on 'reauthentication'" when {
      "authorised" should {
        "redirect to test only /reauthentication url with continue URL" in new RequestWithSessionFixture {
          stubBackendPersonalJourney(Some(nino))
          stubBarsVerifyStatus()
          stubPOSTJourney()

          val response = controller.reauthentication(TdAll.request)
          status(response) shouldBe Status.SEE_OTHER

          redirectLocation(response) shouldBe Some("/self-assessment-refund/test-only/reauthentication?continue=/request-a-self-assessment-refund/reauthenticated-submit")
        }
      }

        def journey(fixture: RequestWithSessionFixture) = Journey(
          sessionId             = Some(fixture.sessionId.value),
          id                    = fixture.journeyId,
          audit                 = AuditFlags(),
          journeyType           = JourneyTypes.RefundJourney,
          amount                = Some(testAmount),
          nino                  = Some(fixture.nino),
          mtdItId               = None,
          paymentMethod         = None,
          accountType           = Some(AccountType("Personal")),
          bankAccountInfo       = Some(BankAccountInfo("name", SortCode("111111"), AccountNumber("12345678"))),
          nrsWebpage            = None,
          hasStartedReauth      = Some(true),
          repaymentConfirmation = Some(RepaymentResponse(OffsetDateTime.parse("2023-12-01T17:35:30+01:00"), RequestNumber("1234567890"))),
          returnUrl             = None
        )

      "return an error if the hasStartedReauth cannot be set" in new RequestWithSessionFixture {
        stubBackendPersonalJourney(Some(nino))
        stubBarsVerifyStatus()
        stubPOSTJourneyError()

        val error = intercept[Exception](await(controller.reauthentication(request)))
        error.getMessage shouldBe "call to set journey failed with status 500"

        verifyUpdateJourneyCalled(journey(this))

      }

      "redirect to GET test only /reauthentication url with continue URL and the hasStartedReauth flag has been successfully set" in new RequestWithSessionFixture {
        stubBarsVerifyStatus()
        stubBackendPersonalJourney(Some(nino))
        stubPOSTJourney()
        stubBarsVerifyStatus()

        val response = controller.reauthentication(request)
        status(response) shouldBe SEE_OTHER
        redirectLocation(response) shouldBe Some("/self-assessment-refund/test-only/reauthentication?continue=/request-a-self-assessment-refund/reauthenticated-submit")

        verifyUpdateJourneyCalled(journey(this))
      }
    }
  }
}
