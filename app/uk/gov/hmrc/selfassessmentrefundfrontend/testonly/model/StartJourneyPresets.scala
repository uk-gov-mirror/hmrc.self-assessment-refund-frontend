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

package uk.gov.hmrc.selfassessmentrefundfrontend.testonly.model

import uk.gov.hmrc.selfassessmentrefundfrontend.model.ReturnUrl
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest.{StartRefund, ViewHistory}

object StartJourneyPresets {

  lazy val default: StartJourneyOptions = fromIndex(0)

  def fromIndex(index: Int): StartJourneyOptions = {
    val req = requests.lift(index).getOrElse(throw new IndexOutOfBoundsException())

    StartJourneyOptions.fromRequest(req)
  }

  val requests: List[StartRequest] = List(
    // Successful StartRefund presets
    StartRefund("AB200111C", 987.65, Option(true), Some(ReturnUrl("/returnUrl"))),
    StartRefund("AB200111D", 123.45, Option(false), Some(ReturnUrl("/returnUrl"))),

    // NRS Failure presets
    StartRefund("AB200400D", 987.65, Option(true), Some(ReturnUrl("/returnUrl"))),
    StartRefund("AB200500D", 987.65, Option(true), Some(ReturnUrl("/returnUrl"))),

    // Successful ViewHistory presets
    ViewHistory("AB111111C"),
    ViewHistory("AB111111D"),
    ViewHistory("AB111111A")
  )
}
