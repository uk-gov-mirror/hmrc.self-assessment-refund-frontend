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

package uk.gov.hmrc.selfassessmentrefundfrontend.testdata

import play.api.mvc.{AnyContentAsEmpty, Cookie}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}
import uk.gov.hmrc.selfassessmentrefundfrontend.TdRepayments
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdSupport.FakeRequestOps

object TdAll extends TdJourney with TdRepayments {

  val authToken = "authorization-value"
  val requestId = "request-id-value"
  val sessionId = "session-deadbeef"

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withAuthToken()
    .withRequestId()
    .withSessionId()

  val welshRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withAuthToken()
    .withRequestId()
    .withSessionId()
    .withCookies(Cookie("PLAY_LANG", "cy"))

}

object TdSupport {

  implicit class FakeRequestOps[T](r: FakeRequest[T]) {

    def withAuthToken(authToken: String = TdAll.authToken): FakeRequest[T] = r.withSession((SessionKeys.authToken, authToken))

    def withRequestId(requestId: String = TdAll.requestId): FakeRequest[T] = r.withHeaders(
      HeaderNames.xRequestId -> requestId
    )

    def withSessionId(sessionId: String = TdAll.sessionId): FakeRequest[T] = r.withSession(
      SessionKeys.sessionId -> sessionId
    )
  }

}
