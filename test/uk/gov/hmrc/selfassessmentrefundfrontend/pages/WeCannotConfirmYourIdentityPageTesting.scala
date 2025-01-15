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

trait WeCannotConfirmYourIdentityPageTesting extends PageContentTesting {
  def checkPageContent(doc: Document): Unit = {

    doc.checkHasNoBackLink()

    doc.checkHasParagraph(
      "You have entered information that does not match our records too many times. For security reasons, you must wait 24 hours and then try again."
    )

    doc.checkHasActionAsButton("http://localhost:9081/report-quarterly/income-and-expenses/view", "Go to tax account")
  }
  def checkPageContentWelsh(doc: Document): Unit = {

    doc.checkHasNoBackLink()

    doc.checkHasParagraph(
      "Rydych wedi nodi gwybodaeth nad yw’n cyd-fynd â’n cofnodion ormod o weithiau. Am resymau diogelwch, mae’n rhaid i chi aros am 24 awr ac yna rhoi cynnig arall arni."
    )

    doc.checkHasActionAsButton("http://localhost:9081/report-quarterly/income-and-expenses/view", "Ewch i’r cyfrif treth")
  }
}
