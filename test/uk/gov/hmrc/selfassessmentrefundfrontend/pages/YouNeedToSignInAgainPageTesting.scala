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

trait YouNeedToSignInAgainPageTesting extends PageContentTesting {

  def checkPageContent(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    doc.checkHasParagraphs(List(
      "You need to sign in again with the same details you used before.",
      "This is for security reasons and to protect you against fraud."
    ))

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.YouNeedToSignInAgainController.onSubmit
    )
  }
  def checkPageContentWelsh(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    doc.checkHasParagraphs(List(
      "Mae angen i chi fewngofnodi eto gan ddefnyddio’r un manylion a ddefnyddiwyd gennych o’r blaen.",
      "Mae hyn am resymau diogelwch ac er mwyn eich diogelu rhag twyll."
    ))

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.YouNeedToSignInAgainController.onSubmit,
      welsh = true
    )
  }
}
