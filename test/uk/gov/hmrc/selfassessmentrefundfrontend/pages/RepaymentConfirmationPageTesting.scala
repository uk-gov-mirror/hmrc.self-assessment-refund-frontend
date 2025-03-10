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
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

trait RepaymentConfirmationPageTesting extends PageContentTesting {
  def checkPageContent(
      amountToBeRepaid:   BigDecimal,
      dateTimeOverride:   OffsetDateTime,
      reference:          String,
      bankAccountNumber:  String,
      isCardOnFile:       Boolean,
      isAgent:            Boolean,
      isClientUtrPresent: Boolean        = false
  )(doc: Document): Unit = {

    val refundByDate = dateTimeOverride.plusDays(38).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    doc.checkHasPanel(
      "Refund request received",
      s"Your refund reference $reference"
    )

    doc.checkHasNoBackLink()

    doc.checkHasSummaryList(
      keyValuePairs =
        if (isAgent && isClientUtrPresent) List(
          ("Recipient", "Jon Smith"),
          ("Unique Taxpayer Reference", "0987654321"),
          ("Refund amount", AmountFormatter.formatAmount(amountToBeRepaid)),
          ("Date requested", dateTimeOverride.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
          ("Tax", "Self Assessment")
        )
        else if (isAgent) List(
          ("Recipient", "Jon Smith"),
          ("Refund amount", AmountFormatter.formatAmount(amountToBeRepaid)),
          ("Date requested", dateTimeOverride.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
          ("Tax", "Self Assessment")
        )
        else List(
          ("Refund amount", AmountFormatter.formatAmount(amountToBeRepaid)),
          ("Date requested", dateTimeOverride.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
          ("Tax", "Self Assessment")
        )
    )

    doc.checkHasH2("What happens next")

    doc.checkHasParagraphs(List(
      if (isCardOnFile && isAgent) s"We will send the refund to the card used to pay your client’s last Self Assessment tax bill. If we cannot do this, we will send the refund to the bank account ending in ${bankAccountNumber.takeRight(3)}." else if (isCardOnFile && !isAgent) s"We will send the refund to the card used to pay your last Self Assessment tax bill. If we cannot do this, we will send the refund to the bank account ending in ${bankAccountNumber.takeRight(3)}." else s"We will send the refund to the bank account ending in ${bankAccountNumber.takeRight(3)}.",
      if (isAgent) s"Your client should get their refund by $refundByDate. To protect you against fraud, HMRC has security measures in place which may cause a delay." else s"You or your agent should get the refund by $refundByDate. To protect you against fraud, HMRC has security measures in place which may cause a delay.",
      "You can check the status of your refund in your HMRC online account.",
      s"If you have not received your refund by $refundByDate, you can contact us. Do not contact us before this date as we will not have an update for you."
    ))

    doc.checkHasHyperlink("Print this page", "#")
    doc.checkHasHyperlink("check the status of your refund", "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker")
    if (isAgent) doc.checkHasHyperlink("contact us", "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/agent-dedicated-line-self-assessment-or-paye-for-individuals") else doc.checkHasHyperlink("contact us", "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment")
    if (isAgent) doc.checkHasActionAsButton("https://www.tax.service.gov.uk/agent-services-account/home", "Return to tax account")
    doc.checkHasHyperlink("What did you think of this service?", "/request-a-self-assessment-refund/feedback")
    doc.checkHasHyperlink("Is this page not working properly? (opens in new tab)", "/contact/report-technical-problem?service=self-assessment-repayment")
  }

  def checkPageContentWelsh(
      amountToBeRepaid:  BigDecimal,
      dateTimeOverride:  OffsetDateTime,
      reference:         String,
      bankAccountNumber: String,
      isCardOnFile:      Boolean,
      isAgent:           Boolean
  )(doc: Document): Unit = {

    val MonthNamesInWelsh = Map(
      1 -> "Ionawr",
      2 -> "Chwefror",
      3 -> "Mawrth",
      4 -> "Ebrill",
      5 -> "Mai",
      6 -> "Mehefin",
      7 -> "Gorffennaf",
      8 -> "Awst",
      9 -> "Medi",
      10 -> "Hydref",
      11 -> "Tachwedd",
      12 -> "Rhagfyr"
    )

    val refundDate = dateTimeOverride.plusDays(38)
    val refundMonth = MonthNamesInWelsh(refundDate.getMonthValue)
    val refundDay = refundDate.getDayOfMonth.toString
    val refundYear = refundDate.getYear.toString
    val refundByDate = s"$refundDay $refundMonth $refundYear"

    val month = MonthNamesInWelsh(dateTimeOverride.getMonthValue)
    val day = dateTimeOverride.getDayOfMonth.toString
    val year = dateTimeOverride.getYear.toString

    doc.checkHasNoBackLink()

    doc.checkHasPanel(
      "Cais am ad-daliad wedi dod i law",
      s"Cyfeirnod eich ad-daliad yw $reference"
    )

    doc.checkHasSummaryList(
      keyValuePairs = if (isAgent) List(
        ("Derbynnydd", "Jon Smith"),
        ("Swm yr ad-daliad", AmountFormatter.formatAmount(amountToBeRepaid)),
        ("Dyddiad y cais", s"$day $month $year"),
        ("Treth", "Hunanasesiad")
      )
      else List(
        ("Swm yr ad-daliad", AmountFormatter.formatAmount(amountToBeRepaid)),
        ("Dyddiad y cais", s"$day $month $year"),
        ("Treth", "Hunanasesiad")
      )
    )

    doc.checkHasH2("Yr hyn sy’n digwydd nesaf")

    doc.checkHasParagraphs(List(
      if (isCardOnFile && isAgent) s"Byddwn yn anfon yr ad-daliad i’r cerdyn a ddefnyddiwyd i dalu bil treth Hunanasesiad diwethaf eich cleient. Os na allwn wneud hyn, byddwn yn anfon yr ad-daliad i’r cyfrif banc sy’n gorffen gyda ${bankAccountNumber.takeRight(3)}." else if (isCardOnFile && !isAgent) s"Byddwn yn anfon yr ad-daliad i’r cerdyn a ddefnyddiwyd i dalu’ch bil treth Hunanasesiad diwethaf. Os na allwn wneud hyn, byddwn yn anfon yr ad-daliad i’r cyfrif banc sy’n gorffen gyda ${bankAccountNumber.takeRight(3)}." else s"Byddwn yn anfon yr ad-daliad i’r cyfrif banc sy’n gorffen gyda ${bankAccountNumber.takeRight(3)}.",
      if (isAgent) s"Dylai’r ad-daliad ddod i law eich cleient erbyn $refundByDate. Er mwyn eich diogelu rhag twyll, mae gan CThEF fesurau diogelwch ar waith a allai achosi oedi." else s"Dylai’r ad-daliad ddod i’ch llaw, neu i law eich asiant, erbyn $refundByDate. Er mwyn eich diogelu rhag twyll, mae gan CThEF fesurau diogelwch ar waith a allai achosi oedi.",
      "Gallwch wirio statws eich ad-daliad yn eich cyfrif ar-lein CThEF.",
      s"Os nad ydych wedi cael eich ad-daliad erbyn $refundByDate, gallwch gysylltu â ni. Peidiwch â chysylltu â ni cyn y dyddiad hwn, gan na fydd gennym ddiweddariad i’w roi i chi."
    ))

    doc.checkHasHyperlink("Argraffu’r dudalen hon", "#")
    doc.checkHasHyperlink("wirio statws eich ad-daliad", "http://localhost:9171/track-a-self-assessment-refund/refund-request-tracker")
    doc.checkHasHyperlink("gysylltu â ni", "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/welsh-language-helplines")
    if (isAgent) doc.checkHasActionAsButton("https://www.tax.service.gov.uk/agent-services-account/home", "Yn ôl i’r cyfrif treth")
    doc.checkHasHyperlink("Rydym yn defnyddio’ch adborth i wella ein gwasanaethau.", "/request-a-self-assessment-refund/feedback")
    doc.checkHasHyperlink("A yw’r dudalen hon yn gweithio’n iawn? (yn agor tab newydd)", "/contact/report-technical-problem?service=self-assessment-repayment")
  }
}
