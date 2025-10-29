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

trait RepaymentStatusPageTesting extends PageContentTesting {

  def checkInProgressPageContent(doc: Document, welsh: Boolean = false): Unit =
    if (welsh) {
      doc.checkHasParagraphs(Paragraphs.inProgressWelsh)
      doc.checkHasBackToTaxAccountButton()
      doc.checkHasBackLinkWithUrl("#")
    } else {
      doc.checkHasParagraphs(Paragraphs.inProgress)
      doc.checkHasBackToTaxAccountButton()
      doc.checkHasBackLinkWithUrl("#")
    }

  def checkRejectedPageContent(doc: Document, welsh: Boolean = false): Unit =
    if (welsh) {
      doc.checkHasParagraphs(Paragraphs.rejectedWelsh)
      doc.checkHasBackToTaxAccountButton()
      doc.checkHasBackLinkWithUrl("#")
    } else {
      doc.checkHasParagraphs(Paragraphs.rejected)
      doc.checkHasBackToTaxAccountButton()
      doc.checkHasBackLinkWithUrl("#")
    }

  def checkPaidPageContent(doc: Document, welsh: Boolean = false): Unit =
    if (welsh) {
      doc.checkHasParagraphs(Paragraphs.paid)
      doc.checkHasActionAsButton(
        "https://return.dummy.com",
        "Yn ôl i’r cyfrif treth"
      )
      doc.checkHasBackLinkWithUrl("#")
    } else {
      doc.checkHasParagraphs(Paragraphs.paid)
      doc.checkHasActionAsButton(
        "https://return.dummy.com",
        "Back to tax account"
      )
      doc.checkHasBackLinkWithUrl("#")
    }

  implicit class RepaymentStatusPageDocTestingSyntax(doc: Document) {
    def checkHasBackToTaxAccountButton(): Unit      =
      doc.checkHasActionAsButton(
        "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
        "Back to tax account"
      )
    def checkHasBackToTaxAccountButtonWelsh(): Unit =
      doc.checkHasActionAsButton(
        "http://localhost:9081/report-quarterly/income-and-expenses/view/claim-refund",
        "Yn ôl i’r cyfrif treth"
      )

  }

  object Paragraphs {
    val inProgress: List[String] = List(
      "We have received your refund request. HMRC aims to issue refunds within 2 weeks, however there are security measures in place which may cause a delay.",
      "Please allow 30 days before contacting us about your request."
    )

    val inProgressWelsh: List[String] = List(
      "Mae’ch cais am ad-daliad wedi dod i law. Bwriad CThEF yw anfon ad-daliadau cyn pen pythefnos, ond mae mesurau diogelwch ar waith a allai achosi oedi.",
      "Dylech aros 30 diwrnod cyn cysylltu â ni ynglŷn â’ch cais."
    )

    val rejected: List[String] = List(
      "We cannot pay your refund of £12,000.00 because your request has been rejected."
    )

    val rejectedWelsh: List[String] = List(
      "We cannot pay your refund of £12,000.00 because your request has been rejected."
    )

    val paid: List[String] = List(
      "We sent you a payment of £12,000.00 on 31 August 2021.",
      "It can take 3 to 5 days for the money to reach your bank account or your card."
    )

    val paidWelsh: List[String] = List(
      "Gwnaethom anfon taliad o £12,000.00 atoch ar 31 August 2021.",
      "Mae’n gallu cymryd 3 i 5 diwrnod i’r arian gyrraedd eich cyfrif banc."
    )
  }
}
