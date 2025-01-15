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
import uk.gov.hmrc.selfassessmentrefundfrontend.model.BankAccountInfo
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter

trait CheckYourAnswersPageTesting extends PageContentTesting {
  def checkPageContent(
      accountType:      String,
      bankAccountInfo:  BankAccountInfo,
      amountToBeRepaid: BigDecimal
  )(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")
    doc.checkHasSummaryList(
      keyValuePairs          = List(
        ("Refund amount", AmountFormatter.formatAmount(amountToBeRepaid)),
        ("Bank account type", accountType),
        ("Bank account details", s"${bankAccountInfo.name} ${bankAccountInfo.sortCode.displayFormat} ${bankAccountInfo.accountNumber.value}"),
      ),
      maybeLinkTextHrefPairs = Some(List(
        ("Change", "/request-a-self-assessment-refund/check-your-answers-change-amount"),
        ("Change", "/request-a-self-assessment-refund/check-your-answers-change-account"),
        ("Change", "/request-a-self-assessment-refund/bank-building-society-details"),
      ))
    )

    doc.checkHasFormAction("Confirm and continue", routes.CheckYourAnswersPageController.confirm)
  }
  def checkPageContentWelsh(
      accountType:      String,
      bankAccountInfo:  BankAccountInfo,
      amountToBeRepaid: BigDecimal
  )(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")
    doc.checkHasSummaryList(
      keyValuePairs          = List(
        ("Swm yr ad-daliad", AmountFormatter.formatAmount(amountToBeRepaid)),
        ("Math o gyfrif banc", accountType),
        ("Manylion cyfrif banc", s"${bankAccountInfo.name} ${bankAccountInfo.sortCode.displayFormat} ${bankAccountInfo.accountNumber.value}"),
      ),
      maybeLinkTextHrefPairs = Some(List(
        ("Newid", "/request-a-self-assessment-refund/check-your-answers-change-amount"),
        ("Newid", "/request-a-self-assessment-refund/check-your-answers-change-account"),
        ("Newid", "/request-a-self-assessment-refund/bank-building-society-details"),
      ))
    )

    doc.checkHasFormAction("Cadarnhau ac yn eich blaen", routes.CheckYourAnswersPageController.confirm)
  }

}
