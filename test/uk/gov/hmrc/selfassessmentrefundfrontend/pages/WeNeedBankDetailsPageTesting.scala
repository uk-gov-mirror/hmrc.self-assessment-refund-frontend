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

trait WeNeedBankDetailsPageTesting extends PageContentTesting {

  def checkPageContent(isAgent: Boolean)(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    if (isAgent) {
      doc.checkHasParagraphs(
        List(
          "To get a refund, you need to enter your client’s bank details."
        )
      )
    } else {
      doc.checkHasParagraphs(
        List(
          "To get a refund, you need to enter your own or your agent’s bank details."
        )
      )
    }

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.WeNeedBankDetailsController.onSubmit
    )
  }
  def checkPageContentWelsh(isAgent: Boolean)(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    if (isAgent) {
      doc.checkHasParagraphs(
        List(
          "I gael ad-daliad, mae angen i chi nodi manylion banc eich cleient."
        )
      )
    } else {
      doc.checkHasParagraphs(
        List(
          "I gael ad-daliad, mae angen i chi nodi eich manylion banc chi, neu rai eich asiant."
        )
      )
    }

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.WeNeedBankDetailsController.onSubmit,
      welsh = true
    )
  }
}
