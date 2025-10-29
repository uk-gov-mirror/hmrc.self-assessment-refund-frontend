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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request

import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.model.NumberOfBarsVerifyAttempts
import uk.gov.hmrc.selfassessmentrefundfrontend.model.ReturnUrl
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.Journey

import java.time.Instant

class LockedOutJourneyRequest[A](
  override val request:           AuthenticatedRequest[A],
  override val journey:           Journey,
  override val sessionId:         SessionId,
  val barsLockoutExpiryTime:      Instant,
  val numberOfBarsVerifyAttempts: NumberOfBarsVerifyAttempts,
  val returnUrl:                  Option[ReturnUrl]
) extends AuthenticatedRequest[A](request, journey, sessionId, request.affinityGroup, request.agentReferenceNumber)
