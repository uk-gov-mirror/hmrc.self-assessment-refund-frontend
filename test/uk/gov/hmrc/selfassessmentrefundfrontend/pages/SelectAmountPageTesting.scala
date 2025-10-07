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
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import support.PageContentTesting
import uk.gov.hmrc.selfassessmentrefundfrontend.model.Amount
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter

trait SelectAmountPageTesting extends PageContentTesting {

  def checkPageContent(amount: Amount, withoutSuggestedAmount: Boolean = false)(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    val suggestedAmount = AmountFormatter.formatOptionalAmount(amount.unallocatedCredit)
    val fullAmount = AmountFormatter.formatOptionalAmount(amount.totalCreditAvailableForRepayment)

    doc.checkHasRadioButtonOptionsWith(List(
      if (withoutSuggestedAmount) None else Some((s"$suggestedAmount", Some("This will leave enough in your tax account to cover your next bill"))),
      Some((s"$fullAmount", None)),
      Some(("A different amount", None))
    ).flatten)

    val conditionalForChoiceDifferent = conditionalElementsForChoiceDifferent(doc)
    conditionalForChoiceDifferent.select(".govuk-label").text() mustBe "Enter an amount"
    conditionalForChoiceDifferent.select(".govuk-hint").text() mustBe s"Enter an amount up to $fullAmount"
    conditionalForChoiceDifferent.select("input").attr("type", "text").attr("name") mustBe "amount"

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.SelectRepaymentAmountController.submitAmount
    )
  }
  def checkPageContentWelsh(amount: Amount, withoutSuggestedAmount: Boolean = false)(doc: Document): Unit = {

    doc.checkHasBackLinkWithUrl("#")

    val suggestedAmount = AmountFormatter.formatOptionalAmount(amount.unallocatedCredit)
    val fullAmount = AmountFormatter.formatOptionalAmount(amount.totalCreditAvailableForRepayment)

    doc.checkHasRadioButtonOptionsWith(List(
      if (withoutSuggestedAmount) None else Some((s"$suggestedAmount", Some("Bydd hyn yn gadael digon yn eich cyfrif treth i dalu’ch bil nesaf"))),
      Some((s"$fullAmount", None)),
      Some(("Swm gwahanol", None))
    ).flatten)

    val conditionalForChoiceDifferent = conditionalElementsForChoiceDifferent(doc)
    conditionalForChoiceDifferent.select(".govuk-label").text() mustBe "Nodwch swm"
    conditionalForChoiceDifferent.select(".govuk-hint").text() mustBe s"Nodwch swm hyd at $fullAmount"
    conditionalForChoiceDifferent.select("input").attr("type", "text").attr("name") mustBe "amount"

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.SelectRepaymentAmountController.submitAmount,
      welsh = true
    )
  }

  def checkPageWithFormError(amount: Amount, errorMessage: String, errorLink: String, withoutSuggestedAmount: Boolean = false)(doc: Document): Unit = {

    checkPageContent(amount, withoutSuggestedAmount = withoutSuggestedAmount)(doc)

    doc.checkHasErrorSummaryWith(errorMessage, errorLink)

    if (errorLink == "#different-amount") conditionalElementsForChoiceDifferent(doc).select(".govuk-error-message").text() mustBe "Error: " + errorMessage
    else doc.select("#choice-error").text() mustBe "Error: " + errorMessage

    ()
  }
  def checkPageWithFormErrorWelsh(amount: Amount, errorMessage: String, errorLink: String, withoutSuggestedAmount: Boolean = false)(doc: Document): Unit = {

    checkPageContentWelsh(amount, withoutSuggestedAmount = withoutSuggestedAmount)(doc)

    doc.checkHasErrorSummaryWithWelsh(errorMessage, errorLink)

    if (errorLink == "#different-amount") conditionalElementsForChoiceDifferent(doc).select(".govuk-error-message").text() mustBe "Gwall: " + errorMessage
    else doc.select("#choice-error").text() mustBe "Gwall: " + errorMessage

    ()
  }

  private def conditionalElementsForChoiceDifferent(doc: Document): Elements = {
    doc.select(".govuk-radios__conditional").select("#conditional-choice-different")
  }

}
