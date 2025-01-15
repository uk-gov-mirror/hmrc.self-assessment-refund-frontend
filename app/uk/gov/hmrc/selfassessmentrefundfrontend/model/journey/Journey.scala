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

package uk.gov.hmrc.selfassessmentrefundfrontend.model.journey

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.AuditFlags
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino

final case class Journey(
    sessionId:             Option[String],
    id:                    JourneyId,
    audit:                 AuditFlags,
    journeyType:           JourneyType,
    amount:                Option[Amount],
    nino:                  Option[Nino],
    mtdItId:               Option[MtdItId],
    paymentMethod:         Option[PaymentMethod],
    accountType:           Option[AccountType],
    bankAccountInfo:       Option[BankAccountInfo],
    nrsWebpage:            Option[String],
    hasStartedReauth:      Option[Boolean],
    repaymentConfirmation: Option[RepaymentResponse],
    returnUrl:             Option[ReturnUrl]
) {
  def toLogMessage: String =
    s"""
       |[sessionId: ${sessionId.getOrElse("sessionId missing")}]
       |[journeyId: ${id.value}]
       |[amount: ${amount.toString}]
       |""".stripMargin
}

object Journey {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit def format: Format[Journey] = Json.format[Journey]
}
