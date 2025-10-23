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

import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.html.components.implicits._
import uk.gov.hmrc.govukfrontend.views.viewmodels.fieldset.Fieldset
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest.{StartRefund, ViewHistory}
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter

import scala.reflect.ClassTag

final case class StartJourneyPageModel(
    form:    Form[StartJourneyOptions],
    presets: List[Preset]
) {

  lazy val journeyType: StartJourneyType = form.value.map(_.`type`).getOrElse(StartJourneyType.StartRefund)

  def submitStartJourney: Call = uk.gov.hmrc.selfassessmentrefundfrontend.testonly.controllers.routes.StartJourneyController.submitStartJourneyForm()

  def selectPreset: Call = uk.gov.hmrc.selfassessmentrefundfrontend.testonly.controllers.routes.StartJourneyController.selectPreset()

  def hideInputsClass: String = journeyType match {
    case StartJourneyType.StartRefund => ""
    case StartJourneyType.ViewHistory => "govuk-visually-hidden"
  }

  def errorSummary(implicit messages: Messages): ErrorSummary = {
    ErrorSummary(
      errorList = form.errors.asTextErrorLinks,
      title     = Text("Issues")
    )
  }

  def makePresetTable[T <: StartRequest: ClassTag]: Table = {
    val baseHeader = Seq(
      HeadCell(),
      HeadCell(content = Text("NINO"))
    )

    val typedPresets: List[(Preset, Int)] =
      presets
        .zipWithIndex
        .collect { case (Preset(v: T, desc), idx) => Preset(v, desc) -> idx }

    val header =
      typedPresets.headOption.map(t => t._1.startRequest -> t._2) match {
        case Some((_: StartRefund, _)) => baseHeader ++ Seq(
          HeadCell(content = Text("Full amount")),
          HeadCell(content = Text("Last payment type")),
          HeadCell(content = Text("Description"))
        )
        case Some((_: ViewHistory, _)) =>
          baseHeader ++ Seq(
            HeadCell(content = Text("Description"))
          )

        case None =>
          throw new IndexOutOfBoundsException()
      }

    val rows: List[Seq[TableRow]] = typedPresets.map {
      case (Preset(req: StartRefund, description: String), idx) => makeStartRefundRow(req, description, idx)
      case (Preset(req: ViewHistory, description: String), idx) => makeViewHistoryRow(req, description, idx)
    }

    Table(
      rows = rows,
      head = Option(header)
    )
  }

  def makeStartRefundRow(req: StartRefund, description: String, n: Int): Seq[TableRow] = {
    val option = makeRadioOption(n.toString, "Start refund")
    val paymentType = if (req.lastPaymentViaCard.getOrElse(false)) {
      "CARD"
    } else {
      "BACS"
    }

    Seq(
      TableRow(content = HtmlContent(option)),
      TableRow(content = Text(req.nino)),
      TableRow(content = Text(AmountFormatter.formatAmount(req.fullAmount))),
      TableRow(content = Text(paymentType)),
      TableRow(content = Text(description))
    )

  }

  def makeViewHistoryRow(req: ViewHistory, description: String, n: Int): Seq[TableRow] = {
    val option = makeRadioOption(n.toString, "View history")

    Seq(
      TableRow(content = HtmlContent(option)),
      TableRow(content = Text(req.nino)),
      TableRow(content = Text(description))
    )

  }

  def makeRadioOption(value: String, label: String): Html = Html {
    s"""<div class="govuk-radios__item">
       |    <input class="govuk-radios__input" id="$value" name="index" type="radio" value="$value">
       |        <label class="govuk-label govuk-radios__label" for="$value">
       |          $label
       |        </label>
       |    </div>""".stripMargin
  }

  def makeLastPaymentItems: Seq[SelectItem] = Seq()

  def makeStubPrimerRadios(implicit messages: Messages): Radios = Radios(
    fieldset = Some(Fieldset(
      legend = Some(Legend(
        content = Text("Prime stubs for this tax account")
      ))
    )),
    hint     = Some(Hint(
      content = Text("What should self-assessment-refund-stubs return for this NINO?")
    )),
    items    = PrimeStubsOption.values.map { opt =>
      RadioItem(
        content = Text(opt.label),
        id      = Some(opt.entryName),
        value   = Some(opt.entryName),
      //          checked = opt == options.primeStubs
      )
    },
    classes  = "govuk-!-full-width govuk-radios--inline govuk-radios--small"
  ).withFormField(form("primeStubs"))

}

object StartJourneyPageModel {

  def apply(form: Form[StartJourneyOptions]): StartJourneyPageModel = {
    StartJourneyPageModel(form, presets = StartJourneyPresets.presets)
  }

}
