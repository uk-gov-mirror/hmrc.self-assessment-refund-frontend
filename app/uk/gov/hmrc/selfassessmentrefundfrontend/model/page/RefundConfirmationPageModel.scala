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

package uk.gov.hmrc.selfassessmentrefundfrontend.model.page

import uk.gov.hmrc.selfassessmentrefundfrontend.model.SaUtr
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber

final case class RefundConfirmationPageModel(
  reference:               RequestNumber,
  theDate:                 String,
  amount:                  String,
  refundByDate:            String,
  bankAccountEndingDigits: String = "",
  bankAccountName:         String = "",
  isLastPaymentByCard:     Boolean = false,
  isAgent:                 Boolean = false,
  clientSaUtr:             SaUtr = SaUtr(None)
)
