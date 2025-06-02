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

package uk.gov.hmrc.selfassessmentrefundfrontend.model.page

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.RefundTrackerPageModel.RefundTrackerModel
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.{RepaymentStatus, RequestNumber}

import java.time.LocalDate

final case class RefundTrackerPageModel(
    refundTracker: List[RefundTrackerModel]
)

object RefundTrackerPageModel {

  final case class RefundTrackerModel(receivedOn: LocalDate, amountClaimed: String, status: RepaymentStatus, key: RequestNumber) {

    def asRow(languageUtils: LanguageUtils)(implicit messages: Messages): List[TableRow] = List(
      TableRow(
        content    = Text(languageUtils.Dates.formatDateAbbrMonth(receivedOn)),
        attributes = Map("scope" -> "row"),
      ), TableRow(
        content = Text(amountClaimed),
        format  = Some("numeric")
      ), TableRow(
        content = HtmlContent(s"""<strong class="govuk-tag govuk-tag--${status.colour}">${messages(status.msgKey)}</strong>"""),
      ),
      TableRow(
        content = HtmlContent(
          s"""<a class="govuk-link" href="${uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney.routes.RepaymentStatusController.statusOf(key).url}">
             |${messages("refund-tracker.view-details")}
             |<span class="govuk-visually-hidden">  ${messages("refund-tracker.view-details.visually-hidden", languageUtils.Dates.formatDate(receivedOn))}</span>
             |</a>""".stripMargin
        )
      )
    )
  }
}
