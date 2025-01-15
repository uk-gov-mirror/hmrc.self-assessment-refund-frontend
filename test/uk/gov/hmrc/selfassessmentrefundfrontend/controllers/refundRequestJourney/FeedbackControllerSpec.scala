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
import play.api.test.Helpers._
import support.ItSpec
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll.nino

class FeedbackControllerSpec extends ItSpec {
  val controller: FeedbackController = fakeApplication().injector.instanceOf[FeedbackController]

  "GET /feedback" when {
    "called" should {
      "redirect to feedback frontend" in {
        givenTheUserIsAuthorised()
        stubBackendPersonalJourney(Some(nino))
        stubBarsVerifyStatus()

        val result = controller.feedbackShow(TdAll.request)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/self-assessment-refund")
      }
    }
  }
}
