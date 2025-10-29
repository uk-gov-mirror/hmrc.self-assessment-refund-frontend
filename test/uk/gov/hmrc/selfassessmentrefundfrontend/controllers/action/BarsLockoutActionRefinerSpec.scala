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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action

import play.api.http.Status
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import support.ItSpec
import support.stubbing.BarsVerifyStatusStub
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.CheckYourAnswersPageController
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

import java.time.Instant

class BarsLockoutActionRefinerSpec extends ItSpec {

  private val controller: CheckYourAnswersPageController = app.injector.instanceOf[CheckYourAnswersPageController]

  "BarsLockoutActionFilter" should {
    "redirect to the 'Check you answers page' when bars verify status does not have a lockout expiry set" in {
      stubBackendBusinessJourney()
      stubPOSTJourney()
      stubBackendJourney()
      BarsVerifyStatusStub.statusUnlocked()

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", "/request-a-self-assessment-refund/check-your-answers")
          .withAuthToken()
          .withSessionId()

      val result = controller.start(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "should return redirect to the lockout page when bars verify status has a lockout expiry set" in {
      val expiry = Instant.now

      stubBackendBusinessJourney(Some(Nino("123456")))
      BarsVerifyStatusStub.statusLocked(expiry)

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", "/request-a-self-assessment-refund/check-your-answers")
          .withAuthToken()
          .withSessionId()

      val result = controller.start(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/request-a-self-assessment-refund/bank-details-tried-too-many-times")
    }
  }
}
