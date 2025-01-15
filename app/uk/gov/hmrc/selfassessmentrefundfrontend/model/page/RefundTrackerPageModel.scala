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
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.RefundTrackerPageModel.RefundTrackerModel
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.{RepaymentStatus, RequestNumber}
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService._
import uk.gov.hmrc.selfassessmentrefundfrontend.util.{AmountFormatter, Mapping}
import uk.gov.hmrc.selfassessmentrefundfrontend.util.Mapping.ConversionOps

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

  object RefundTrackerModel {

    implicit val processingConversion: Mapping[ProcessingTaxRepayment, RefundTrackerModel] = new Mapping[ProcessingTaxRepayment, RefundTrackerModel] {
      override def mapFrom(a: ProcessingTaxRepayment): RefundTrackerModel = RefundTrackerModel(a.claim.created, AmountFormatter.formatAmount(a.claim.amount), RepaymentStatus.Processing, a.claim.key)
    }

    implicit val processingRiskingConversion: Mapping[ProcessingRiskingTaxRepayment, RefundTrackerModel] = new Mapping[ProcessingRiskingTaxRepayment, RefundTrackerModel] {
      override def mapFrom(a: ProcessingRiskingTaxRepayment): RefundTrackerModel = RefundTrackerModel(a.claim.created, AmountFormatter.formatAmount(a.claim.amount), RepaymentStatus.ProcessingRisking, a.claim.key)
    }

    implicit val approvedConversion: Mapping[ApprovedTaxRepayment, RefundTrackerModel] = new Mapping[ApprovedTaxRepayment, RefundTrackerModel] {
      override def mapFrom(a: ApprovedTaxRepayment): RefundTrackerModel = RefundTrackerModel(a.claim.created, AmountFormatter.formatAmount(a.claim.amount), RepaymentStatus.Approved, a.claim.key)
    }

    implicit val rejectedConversion: Mapping[RejectedTaxRepayment, RefundTrackerModel] = new Mapping[RejectedTaxRepayment, RefundTrackerModel] {
      override def mapFrom(a: RejectedTaxRepayment): RefundTrackerModel = RefundTrackerModel(a.claim.created, AmountFormatter.formatAmount(a.claim.amount), RepaymentStatus.Rejected, a.claim.key)
    }

    def header(implicit messages: Messages): List[HeadCell] = List(
      HeadCell(
        content = Text(messages("refund-tracker.head.request-date")),
        format  = Some("")
      ),
      HeadCell(
        content = Text(messages("refund-tracker.head.amount")),
        format  = Some("numeric")
      ),
      HeadCell(
        content = Text(messages("refund-tracker.head.status"))
      ),
      HeadCell(
        content = HtmlContent(s"""<span class="govuk-visually-hidden">${messages("refund-tracker.head.actions")}</span>""")
      )
    )
  }

  implicit val conversion: Mapping[List[TaxRepayment], RefundTrackerPageModel] = new Mapping[List[TaxRepayment], RefundTrackerPageModel] {
    override def mapFrom(a: List[TaxRepayment]): RefundTrackerPageModel = {

      RefundTrackerPageModel(
        a.collect {
          case p: ProcessingRiskingTaxRepayment => p.mapTo[RefundTrackerModel]
          case p: ProcessingTaxRepayment        => p.mapTo[RefundTrackerModel]
          case p: RejectedTaxRepayment          => p.mapTo[RefundTrackerModel]
          case p: ApprovedTaxRepayment          => p.mapTo[RefundTrackerModel]
        }.sortWith((a, b) => a.receivedOn.compareTo(b.receivedOn) > 0),
      )
    }
  }

}
