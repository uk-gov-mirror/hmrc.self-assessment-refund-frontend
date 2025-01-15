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

trait YourRefundRequestNotSubmittedPageTesting extends PageContentTesting {

  def checkPageContent(isAgent: Boolean)(doc: Document): Unit = {
    doc.checkHasNoBackLink()
    doc.checkHasParagraphs(List(
      "There was a problem processing your refund request.",
      "You will need to start again."
    ))

    doc.checkHasActionAsButton(
      if (isAgent) "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund" else "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
      "Start again"
    )
  }
  def checkPageContentWelsh(isAgent: Boolean)(doc: Document): Unit = {
    doc.checkHasNoBackLink()
    doc.checkHasParagraphs(List(
      "Roedd problem wrth brosesu’ch cais am ad-daliad.",
      "Bydd angen i chi ddechrau eto."
    ))

    doc.checkHasActionAsButton(
      if (isAgent) "http://localhost:9081/report-quarterly/income-and-expenses/view/agents/claim-refund" else "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
      "Dechrau eto"
    )
  }
}
