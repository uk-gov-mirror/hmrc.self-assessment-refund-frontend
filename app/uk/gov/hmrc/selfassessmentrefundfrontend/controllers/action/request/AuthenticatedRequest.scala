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

import play.api.i18n.{Messages, MessagesApi}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.Journey

class AuthenticatedRequest[A](
    override val request:   PreAuthRequest[A],
    override val journey:   Journey,
    override val sessionId: SessionId,
    val affinityGroup:      AffinityGroup
) extends PreAuthRequest[A](request, journey, sessionId) {
  val isAgent: Boolean = affinityGroup match {
    case Agent                     => true
    case Individual | Organisation => false
    case _                         => sys.error("Unsupported affinity group.")
  }
}

object AuthenticatedRequest {
  implicit def request2Messages(implicit request: AuthenticatedRequest[_], messagesApi: MessagesApi): Messages = messagesApi.preferred(request)
}
