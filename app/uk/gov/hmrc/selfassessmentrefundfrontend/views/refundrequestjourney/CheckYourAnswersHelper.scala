/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentrefundfrontend.views.refundrequestjourney

import cats.implicits.catsSyntaxEq
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Key, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryList}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.{AccountType, Amount, BankAccountInfo}
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter

import javax.inject.{Inject, Singleton}

@Singleton
class CheckYourAnswersHelper @Inject() (i18n: I18nSupport) {
  import i18n._

  def buildSummaryList(amount: Amount, accountType: AccountType, bankAccountInfo: BankAccountInfo)(implicit request: Request[_]): SummaryList = SummaryList(rows = Seq(
    amountRow(amount),
    accountTypeRow(accountType),
    accountDetailsRow(bankAccountInfo)
  ))

  private def amountRow(amount: Amount)(implicit request: Request[_]): SummaryListRow = {
    buildSummaryListRow(
      key   = Messages("check-your-answers.amount"),
      value = AmountFormatter.formatAmount(amount.repay),
      call  = uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.CheckYourAnswersPageController.changeAmount
    )
  }

  private def accountTypeRow(accountType: AccountType)(implicit request: Request[_]): SummaryListRow = {
    buildSummaryListRow(
      key   = Messages("check-your-answers.bank-account-type"),
      value = if (accountType.name === "Personal") {
        Messages("check-your-answers.personal")
      } else Messages("check-your-answers.business"),
      call  = uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.CheckYourAnswersPageController.changeAccount
    )
  }

  private def accountDetailsRow(bankAccountInfo: BankAccountInfo)(implicit request: Request[_]): SummaryListRow = {
    buildSummaryListRow(
      key   = Messages("check-your-answers.bank-account-details"),
      value = bankAccountInfoDisplayFormat(bankAccountInfo),
      call  = uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.BankAccountDetailsController.getAccountDetails
    )
  }

  private def buildSummaryListRow(key: String, value: String, call: Call)(implicit request: Request[_]): SummaryListRow = SummaryListRow(
    key     = Key(Text(s"""$key""")),
    value   = Value(HtmlContent(value)),
    actions = Some(uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Actions(items = Seq(ActionItem(href               = s"${call.url}", content = Text(Messages("check-your-answers.change")), visuallyHiddenText = Some(key)))))
  )

  private def bankAccountInfoDisplayFormat(bankAccountInfo: BankAccountInfo): String = {
    s"""
       |${bankAccountInfo.name}<br>
       |${bankAccountInfo.sortCode.displayFormat}<br>
       |${bankAccountInfo.accountNumber.value}<br>
       |${bankAccountInfo.rollNumber.map(_.value).getOrElse("")}
       |""".stripMargin.trim
  }
}
