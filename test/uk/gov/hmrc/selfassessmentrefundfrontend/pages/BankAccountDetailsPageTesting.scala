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

import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers.mustBe
import support.PageContentTesting

import scala.jdk.CollectionConverters.IteratorHasAsScala

trait BankAccountDetailsPageTesting extends PageContentTesting {
  def checkPageContent(doc: Document): Unit = {
    val fields = doc.select(".govuk-form-group").select(".form-field-group").iterator().asScala.toList

    val labels = fields.map(_.select(".govuk-label").text())
    val hints = fields.map(_.select(".govuk-hint").text())
    val fieldNames = textInputElementList(doc).map(_.attr("name"))

    doc.checkHasBackLinkWithUrl("#")

    labels mustBe List(
      "Name on the account",
      "Sort code",
      "Account number",
      "Building society roll number (if you have one)"
    )
    hints mustBe List(
      "",
      "Must be 6 digits long",
      "Must be between 6 and 8 digits long",
      "You can find it on your card, statement or passbook"
    )

    fieldNames mustBe List(
      "accountName",
      "sortCode",
      "accountNumber",
      "rollNumber"
    )

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails
    )

  }
  def checkPageContentWelsh(doc: Document): Unit = {
    val fields = doc.select(".govuk-form-group").select(".form-field-group").iterator().asScala.toList

    val labels = fields.map(_.select(".govuk-label").text())
    val hints = fields.map(_.select(".govuk-hint").text())
    val fieldNames = textInputElementList(doc).map(_.attr("name"))

    doc.checkHasBackLinkWithUrl("#")

    labels mustBe List(
      "Yr enw sydd ar y cyfrif",
      "Cod didoli",
      "Rhif y cyfrif",
      "Rhif rôl y gymdeithas adeiladu (os oes gennych un)"
    )
    hints mustBe List(
      "",
      "Mae’n rhaid iddo fod yn 6 digid o hyd",
      "Mae’n rhaid iddo fod rhwng 6 ac 8 digid o hyd",
      "Bydd hwn i’w weld ar eich cerdyn, cyfriflen neu baslyfr"
    )

    fieldNames mustBe List(
      "accountName",
      "sortCode",
      "accountNumber",
      "rollNumber"
    )

    doc.checkHasFormActionAsContinueButton(
      uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails,
      welsh = true
    )

  }

  def checkPageWithFormError(errorMessage: String, errorField: String, errorLink: String)(doc: Document): Unit = {

    checkPageContent(doc)

    doc.checkHasErrorSummaryWith(errorMessage, errorLink)

    doc.checkHasErrorMessageAgainst(errorField, errorMessage)
  }

  def checkPageWithFormErrorWelsh(errorMessage: String, errorField: String, errorLink: String)(doc: Document): Unit = {

    checkPageContentWelsh(doc)

    doc.checkHasErrorSummaryWithWelsh(errorMessage, errorLink)

    doc.checkHasErrorMessageAgainst(errorField, errorMessage, welsh = true)
  }

  def checkPageWithFormError(errorFieldMessagePairs: Map[ErrorField, ErrorMessage])(doc: Document): Unit = {

    checkPageContent(doc)

    val errorMessages = errorFieldMessagePairs.map { case (_, errorMessage: String) => errorMessage }

    doc.checkHasMultipleErrorsWith(errorMessages.mkString(" "))

    for (elem <- errorFieldMessagePairs) {
      doc.checkHasErrorMessageAgainst(elem._1, elem._2)

    }

  }

  def checkPageWithFormErrorWelsh(errorFieldMessagePairs: Map[ErrorField, ErrorMessage])(doc: Document): Unit = {

    checkPageContentWelsh(doc)

    val errorMessages = errorFieldMessagePairs.map { case (_, errorMessage: String) => errorMessage }

    doc.checkHasMultipleErrorsWithWelsh(errorMessages.mkString(" "))

    for (elem <- errorFieldMessagePairs) {
      doc.checkHasErrorMessageAgainst(elem._1, elem._2, welsh = true)

    }

  }

  private def textInputElementList(doc: Document): List[Element] = {
    doc.select("input").attr("type", "text").iterator().asScala.toList
  }

  type ErrorField = String
  type ErrorMessage = String

}
