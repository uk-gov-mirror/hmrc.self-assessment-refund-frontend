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

package uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.startclaimjourney

import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.AuditOutcome
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino

final case class RefundAmountAuditDetail(
  outcome:                          AuditOutcome,
  totalCreditAvailableForRepayment: Option[BigDecimal],
  unallocatedCredit:                Option[BigDecimal],
  amountChosen:                     Option[BigDecimal],
  nino:                             Option[Nino],
  agentReferenceNumber:             Option[String],
  userType:                         Option[AffinityGroup]
)

object RefundAmountAuditDetail {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val writes: OWrites[RefundAmountAuditDetail] = Json.writes[RefundAmountAuditDetail]
}
