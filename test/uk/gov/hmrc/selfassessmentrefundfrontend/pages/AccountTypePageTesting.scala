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
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes

trait AccountTypePageTesting extends PageContentTesting {

  def checkPageContent(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    doc.checkHasHint("The bank details you provide must be for a UK bank account")

    doc.checkHasRadioButtonOptionsWith(
      List(
        ("Business bank account", None),
        ("Personal bank account", None)
      )
    )

    doc.checkHasFormActionAsContinueButton(routes.AccountTypeController.postAccountType)
  }

  def checkPageWithFormError(doc: Document): Unit = {
    checkPageContent(doc)

    doc.checkHasErrorSummaryWith("Select a type of account", "#accountType")

    doc.checkHasErrorMessageAgainst("accountType", "Select a type of account")
  }
  def checkPageContentWelsh(doc: Document): Unit  = {

    doc.checkHasBackLinkWithUrl("#")

    doc.checkHasHint("Mae’n rhaid i’r manylion banc rydych yn eu darparu fod ar gyfer cyfrif banc yn y DU")

    doc.checkHasRadioButtonOptionsWith(
      List(
        ("Cyfrif banc busnes", None),
        ("Cyfrif banc personol", None)
      )
    )

    doc.checkHasFormActionAsContinueButton(routes.AccountTypeController.postAccountType, true)
  }

  def checkPageWithFormErrorWelsh(doc: Document): Unit = {
    checkPageContentWelsh(doc)

    doc.checkHasErrorSummaryWithWelsh("Dewiswch y math o gyfrif", "#accountType")

    doc.checkHasErrorMessageAgainst("accountType", "Dewiswch y math o gyfrif", welsh = true)
  }

}
