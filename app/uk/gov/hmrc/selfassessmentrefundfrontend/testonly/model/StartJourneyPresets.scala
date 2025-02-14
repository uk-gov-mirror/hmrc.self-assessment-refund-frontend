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

final case class Preset(
    startRequest: StartRequest,
    description:  String
)

object StartJourneyPresets {

  lazy val default: StartJourneyOptions = fromIndex(0)

  def fromIndex(index: Int): StartJourneyOptions = {
    val preset =
      presets
        .lift(index)
        .getOrElse(throw new IndexOutOfBoundsException())

    StartJourneyOptions.fromRequest(preset.startRequest)
  }

  private val CARD: Option[Boolean] = Option(true)
  private val BACS: Option[Boolean] = Option(false)

  private val defaultReturnUrl: Option[ReturnUrl] = Some(ReturnUrl("/returnUrl"))

  // Successful StartRefund presets
  val presets: List[Preset] = List(
    Preset(StartRefund("AB200111C", 987.65, CARD, defaultReturnUrl), "Happy path, with last payment by CARD"),
    Preset(StartRefund("AB200111D", 123.45, BACS, defaultReturnUrl), "Happy path, with last payment by BACS"),
    Preset(StartRefund("AB200131C", 345.67, CARD, defaultReturnUrl), "API 1553 - balanceDueWithin30Days > availableCredit"),
    Preset(StartRefund("AB200141C", 345.67, CARD, defaultReturnUrl), "API 1553 - balanceDueWithin30Days = availableCredit"),
    Preset(StartRefund("AB500111C", 987.65, CARD, defaultReturnUrl), "API 1553 - Get Financial Details returns error"),
    Preset(StartRefund("AB200111B", 987.65, CARD, defaultReturnUrl), "API 1770 Send Repayment Request returns error"),
    Preset(StartRefund("AB200400D", 987.65, CARD, defaultReturnUrl), "NRS Submission returns 400"),
    Preset(StartRefund("AB200500D", 987.65, CARD, defaultReturnUrl), "NRS Submission returns 500"),

    // Successful ViewHistory presets
    Preset(ViewHistory("AB111111A"), "No payments, you have not yet requested a refund"),
    Preset(ViewHistory("AB111111C"), "4 payments (round numbers)"),
    Preset(ViewHistory("AB111111D"), "4 payments (pounds and pence)")
  )
}
