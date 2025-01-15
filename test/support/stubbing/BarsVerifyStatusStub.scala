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

package support.stubbing

import com.github.tomakehurst.wiremock.client.WireMock.{exactly, postRequestedFor, urlPathEqualTo, verify}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import support.stubbing.WireMockHelpers.stubForPostWithResponseBody

import java.time.Instant

object BarsVerifyStatusStub {
  private def noLockoutBody(numberOfAttempts: Int) = s"""{
                                                        |    "attempts": ${numberOfAttempts.toString}
                                                        |}""".stripMargin

  private def lockoutBody(expiry: Instant) = s"""{
                                                |    "attempts": 3,
                                                |    "lockoutExpiryDateTime": "${expiry.toString}"
                                                |}""".stripMargin

  private val getVerifyStatusUrl: String = "/self-assessment-refund-backend/bars/verify/status"
  private val updateVerifyStatusUrl: String = "/self-assessment-refund-backend/bars/verify/update"

  def statusUnlocked(): StubMapping = stubPost(getVerifyStatusUrl, noLockoutBody(numberOfAttempts = 1))

  def statusLocked(expiry: Instant): StubMapping = stubPost(getVerifyStatusUrl, lockoutBody(expiry))

  def update(numberOfAttempts: Int = 1): StubMapping = stubPost(updateVerifyStatusUrl, noLockoutBody(numberOfAttempts))

  def updateAndLockout(expiry: Instant): StubMapping = stubPost(updateVerifyStatusUrl, lockoutBody(expiry))

  def ensureVerifyUpdateStatusIsCalled(): Unit = {
    verify(exactly(1), postRequestedFor(urlPathEqualTo(updateVerifyStatusUrl)))
  }

  def ensureVerifyUpdateStatusIsNotCalled(): Unit =
    verify(exactly(0), postRequestedFor(urlPathEqualTo(updateVerifyStatusUrl)))

  private def stubPost(url: String, responseJson: String, status: Int = OK): StubMapping =
    stubForPostWithResponseBody(url, responseJson, status)
}
