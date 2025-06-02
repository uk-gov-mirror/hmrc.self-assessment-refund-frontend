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

package uk.gov.hmrc.selfassessmentrefundfrontend.pages

import org.jsoup.nodes.Document
import support.PageContentTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber

trait RefundTrackerPageTesting extends PageContentTesting {
  def checkPageContent(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")
    doc.checkHasTabs(List("2023", "2021"))
    doc.checkHasTable(
      columnHeaders = List("Request date", "Amount", "Status", "Action"),
      rowHeaders    = List(),
      cells         = List(
        "16 Aug 2023", "£76,000", "Approved", "View",
        "14 Aug 2021", "£12,000", "Processing", "View"
      )
    )

    doc.checkHasHyperlink(
      "View details for the refund requested on 14 August 2021",
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney.routes.RepaymentStatusController.statusOf(RequestNumber("1")).url
    )

    doc.checkHasHyperlink(
      "View details for the refund requested on 16 August 2023",
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney.routes.RepaymentStatusController.statusOf(RequestNumber("2")).url
    )
  }

  def checkNoHistoryPageContent(isAgent: Boolean, welsh: Boolean)(doc: Document): Unit = {
    doc.checkHasBackLinkWithUrl("#")

    if (welsh) {
      doc.checkHasParagraphs(List("Nid ydych wedi gwneud cais am ad-daliad Hunanasesiad eto.", "Os oes gennych gredyd yn eich cyfrif ar-lein, gallwch wneud cais am ad-daliad."))

      doc.checkHasHyperlink(
        "wneud cais am ad-daliad",
        if (isAgent) "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund" else "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund"
      )
    } else {
      doc.checkHasParagraphs(List("You have not yet requested a Self Assessment refund.", "If you have credit in your online account, you can request a refund."))

      doc.checkHasHyperlink(
        "request a refund",
        if (isAgent) "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund" else "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund"
      )
    }
  }

  def checkPageContentWelsh(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")
    doc.checkHasTabs(List("2023", "2021"))
    doc.checkHasTable(
      columnHeaders = List("Dyddiad y cais", "Swm", "Statws", "Camau"),
      rowHeaders    = List(),
      cells         = List(
        "16 Awst 2023", "£76,000", "Wedi’i gymeradwyo", "golwg",
        "14 Awst 2021", "£12,000", "Wrthi’n prosesu", "golwg"
      )
    )

    doc.checkHasHyperlink(
      "Bwrw golwg dros y manylion ar gyfer y cais am ad-daliad, dyddiedig 14 Awst 2021",
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney.routes.RepaymentStatusController.statusOf(RequestNumber("1")).url
    )

    doc.checkHasHyperlink(
      "Bwrw golwg dros y manylion ar gyfer y cais am ad-daliad, dyddiedig 16 Awst 2023",
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.trackRefundJourney.routes.RepaymentStatusController.statusOf(RequestNumber("2")).url
    )
  }
}
