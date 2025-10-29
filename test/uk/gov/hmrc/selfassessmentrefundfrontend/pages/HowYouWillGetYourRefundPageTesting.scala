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

trait HowYouWillGetYourRefundPageTesting extends PageContentTesting {

  def checkPageContent(isAgent: Boolean)(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    if (isAgent) {
      doc.checkHasList(
        List(
          "We will send the refund to the card used to pay your client’s last Self Assessment tax bill.",
          "If we cannot do this, we need your client’s bank details as a backup."
        )
      )
    } else {
      doc.checkHasList(
        List(
          "We will send the refund to the card used to pay your last Self Assessment tax bill.",
          "If we cannot do this, we need your bank details as a backup.",
          "You can enter your own or your agent’s bank details."
        )
      )
    }

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.HowYouWillGetYourRefundController.onSubmit
    )

  }
  def checkPageContentWelsh(isAgent: Boolean)(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    if (isAgent) {
      doc.checkHasList(
        List(
          "Byddwn yn anfon yr ad-daliad i’r cerdyn a ddefnyddiwyd i dalu bil treth Hunanasesiad diwethaf eich cleient.",
          "Os na allwn wneud hyn, bydd angen arnom fanylion banc eich cleient, fel opsiwn wrth gefn."
        )
      )
    } else {
      doc.checkHasList(
        List(
          "Byddwn yn anfon yr ad-daliad i’r cerdyn a ddefnyddiwyd i dalu’ch bil treth Hunanasesiad diwethaf.",
          "Os na allwn wneud hyn, bydd angen arnom eich manylion banc, fel opsiwn wrth gefn.",
          "Gallwch nodi eich manylion banc chi, neu rai eich asiant."
        )
      )
    }

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.HowYouWillGetYourRefundController.onSubmit,
      welsh = true
    )

  }
}
