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

package uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment

sealed trait RepaymentStatus {
  val id: String
  val colour: String
  def msgKey: String = s"refund-tracker.status.$id"
}

object RepaymentStatus {
  case object Processing extends RepaymentStatus {
    override val id: String = "processing"
    override val colour: String = "blue"
  }

  case object ProcessingRisking extends RepaymentStatus {
    override val id: String = "processing"
    override val colour: String = "blue"
  }

  case object Approved extends RepaymentStatus {
    override val id: String = "approved"
    override val colour: String = "turquoise"
  }

  case object Rejected extends RepaymentStatus {
    override val id: String = "rejected"
    override val colour: String = "red"
  }
}
