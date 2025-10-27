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

package support

import org.jsoup.Jsoup
import org.jsoup.helper.Validate.fail
import org.jsoup.nodes.Document
import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.matchers.must.Matchers.{endWith, include}
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

trait PageContentTesting {

  implicit class ResultsTestingSyntax(result: Future[Result]) {

    def checkPageIsDisplayed(
      expectedHeading:          String,
      expectedServiceLink:      String,
      contentChecks:            Document => Unit = _ => (),
      expectedStatus:           Int = OK,
      withError:                Boolean = false,
      expectedTitleIfDifferent: Option[String] = None,
      journey:                  String = "",
      withBackButton:           Boolean = true,
      welsh:                    Boolean = false
    ): Unit = {

      status(result) shouldBe expectedStatus
      redirectLocation(result) shouldBe None

      val doc = Jsoup.parse(contentAsString(result))

      doc.checkPageTitle(expectedHeading, withError, expectedTitleIfDifferent, journey, welsh)
      doc.checkPageHeading(expectedHeading)
      doc.checkServiceHeadingLink(expectedServiceLink)
      doc.checkForMissingMessageKeys()
      doc.checkHasLanguageToggle()
      doc.checkHasSignOutLink()
      contentChecks(doc)

      if (withBackButton) doc.checkHasBackLink(welsh)
    }
  }

  implicit class DocumentTestingSyntax(doc: Document) {

    def checkHasLanguageToggle(): Unit = {
      doc.select(".hmrc-language-select__list").text should include regex "English"
      doc.select(".hmrc-language-select__list").text should include regex "Cymraeg"
      ()
    }

    def checkHasBackLink(welsh: Boolean = false): Unit = {
      if (welsh) {
        doc.select("a").select(".govuk-back-link").text() shouldBe "Yn ôl"
      } else doc.select("a").select(".govuk-back-link").text() shouldBe "Back"
      ()
    }

    def checkHasBackLinkWithUrl(url: String): Unit = {
      doc.select("a").select(".govuk-back-link").attr("href") should endWith(url)
      ()
    }

    def checkHasNoBackLink(): Unit = {
      doc.select("a").select(".govuk-back-link").attr("href") shouldBe ""
      ()
    }

    def checkServiceHeadingLink(url: String): Unit = {
      doc.select("a").select(".govuk-header__service-name").attr("href") should endWith(url)
      ()
    }

    def checkHasSignOutLink(): Unit = {
      val signOutUrl: String =
        "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9171/self-assessment-refund/test-only"
      doc.select("a").select(".hmrc-sign-out-nav__link").attr("href") shouldBe signOutUrl
      ()
    }

    def checkForMissingMessageKeys(): Unit = {
      val bodyText = doc.select("body").text
      val regex    = """not_found_message\((.*?)\)""".r

      val regexResult = regex.findAllMatchIn(bodyText).toList
      if (regexResult.nonEmpty) fail(s"Missing message keys: ${regexResult.map(_.group(1)).mkString(", ")}")
    }

    def checkPageHeading(expectedHeading: String): Unit = {
      doc.select("h1").text shouldBe expectedHeading
      ()
    }

    def checkPageTitle(
      expectedHeading:          String,
      withError:                Boolean = false,
      expectedTitleIfDifferent: Option[String] = None,
      journey:                  String = "",
      welsh:                    Boolean
    ): Unit = {
      welsh match {
        case true  =>
          val expectedTitleContent = expectedTitleIfDifferent.fold(expectedHeading)(identity)
          val optionalErrorPrefix  = if (withError) "Gwall: " else ""

          def serviceName =
            if (journey.equals("request")) { " - Gwneud cais am ad-daliad Hunanasesiad" }
            else if (journey.equals("track")) { " - Olrhain ad-daliad Hunanasesiad" }
            else { "" }

          val expectedTitle = optionalErrorPrefix + expectedTitleContent + serviceName + " - GOV.UK"
          doc.title() shouldBe expectedTitle
        case false =>
          val expectedTitleContent = expectedTitleIfDifferent.fold(expectedHeading)(identity)
          val optionalErrorPrefix  = if (withError) "Error: " else ""

          def serviceName =
            if (journey.equals("request")) { " - Request a Self Assessment refund" }
            else if (journey.equals("track")) { " - Track a Self Assessment refund" }
            else { "" }

          val expectedTitle = optionalErrorPrefix + expectedTitleContent + serviceName + " - GOV.UK"
          doc.title() shouldBe expectedTitle
      }
      ()
    }

    def checkHasParagraph(text: String): Unit = {
      val pContent = doc.select("p").iterator().asScala
      pContent.exists(element => element.text() == text) shouldBe true
      ()
    }

    def checkHasH2(text: String): Unit = {
      val pContent = doc.select("h2").iterator().asScala
      pContent.exists(element => element.text() == text) shouldBe true
      ()
    }

    def checkHasParagraphs(text: List[String]): Unit = {
      val pContent = doc.select("p").iterator().asScala.map(_.text()).toList
      for (p <- text) pContent.contains(p) shouldBe true withClue s"Expected: [$p] in ${pContent.toString()}"
      ()
    }

    def checkHasHint(text: String): Unit = {
      val hintContent = doc.select(".govuk-hint").iterator().asScala
      hintContent.exists(element => element.text() == text) shouldBe true
      ()
    }

    def checkHasList(text: List[String]): Unit = {
      val listContent = doc.select("li").iterator().asScala
      for (listItem <- text) listContent.exists(element => element.text() == listItem) shouldBe true
      ()
    }

    def checkHasHyperlink(text: String, link: String): Unit = {
      val links = doc.select(".govuk-link").iterator().asScala.toList
      links.find(_.text() == text).exists(_.attr("href") == link) shouldBe true
      ()
    }

    def checkHasRadioButtonOptionsWith(expectedLabelHintPairs: List[(String, Option[String])]): Unit = {
      val radios = doc.select(".govuk-radios__item").iterator().asScala.toList
      val labels = radios.map(_.select(".govuk-label").text())
      val hints  = radios.map(r => Option(r.select(".govuk-hint").text()).filter(_.nonEmpty))

      for (i <- expectedLabelHintPairs.indices) {
        labels(i) shouldBe expectedLabelHintPairs(i)._1
        hints(i) shouldBe expectedLabelHintPairs(i)._2
      }
      ()
    }

    def checkHasErrorSummaryWith(errorMessage: String, errorLink: String): Unit = {
      val errorSummaryPrefix = doc.select(".govuk-error-summary__title")
      errorSummaryPrefix.text() shouldBe "There is a problem"

      val errorSummary = doc.select(".govuk-error-summary")
      errorSummary.select("a").text() shouldBe errorMessage
      errorSummary.select("a").attr("href") shouldBe errorLink

      val inputErrorMessage = doc.select(".govuk-error-message")
      inputErrorMessage.text().replaceAll("Error: ", "") shouldBe s"$errorMessage"
      ()
    }

    def checkHasMultipleErrorsWith(errorMessage: String): Unit = {
      val errorSummaryPrefix = doc.select(".govuk-error-summary__title")
      errorSummaryPrefix.text() shouldBe "There is a problem"

      val errorSummary = doc.select(".govuk-error-summary")
      errorSummary.select("a").text() shouldBe errorMessage

      val inputErrorMessage = doc.select(".govuk-error-message")
      inputErrorMessage.text().replaceAll("Error: ", "") shouldBe s"$errorMessage"
      ()
    }

    def checkHasErrorSummaryWithWelsh(errorMessage: String, errorLink: String): Unit = {
      val errorSummaryPrefix = doc.select(".govuk-error-summary__title")
      errorSummaryPrefix.text() shouldBe "Mae problem wedi codi"

      val errorSummary = doc.select(".govuk-error-summary")
      errorSummary.select("a").text() shouldBe errorMessage
      errorSummary.select("a").attr("href") shouldBe errorLink

      val inputErrorMessage = doc.select(".govuk-error-message")
      inputErrorMessage.text().replaceAll("Gwall: ", "") shouldBe s"$errorMessage"
      ()
    }

    def checkHasMultipleErrorsWithWelsh(errorMessage: String): Unit = {
      val errorSummaryPrefix = doc.select(".govuk-error-summary__title")
      errorSummaryPrefix.text() shouldBe "Mae problem wedi codi"

      val errorSummary = doc.select(".govuk-error-summary")
      errorSummary.select("a").text() shouldBe errorMessage

      val inputErrorMessage = doc.select(".govuk-error-message")
      inputErrorMessage.text().replaceAll("Gwall: ", "") shouldBe s"$errorMessage"
      ()
    }

    def checkHasErrorMessageAgainst(errorField: String, errorMessage: String, welsh: Boolean = false): Unit = {
      if (welsh) {
        doc.select(s"#$errorField-error").text().replaceAll("Gwall: ", "") shouldBe errorMessage
      } else doc.select(s"#$errorField-error").text().replaceAll("Error: ", "") shouldBe errorMessage
      ()
    }

    def checkHasFormActionAsContinueButton(call: Call, welsh: Boolean = false): Unit =
      if (welsh) {
        checkHasFormAction("Yn eich blaen", call)
      } else checkHasFormAction("Continue", call)

    def checkHasFormAction(buttonText: String, call: Call): Unit = {
      val action = doc.select("form")
      action.attr("method") shouldBe call.method
      action.attr("action") shouldBe call.url
      doc.select("button").text() shouldBe buttonText
      ()
    }

    def checkHasActionAsButton(href: String, buttonText: String): Unit = {
      doc
        .select("a")
        .select(".govuk-button")
        .select(s"""[href="$href"]""")
        .text() shouldBe buttonText
      ()
    }

    def checkHasPanel(title: String, body: String): Unit = {
      doc.select(".govuk-panel__title").text() shouldBe title
      doc.select(".govuk-panel__body").text() shouldBe body
      ()
    }

    def checkHasSummaryList(
      keyValuePairs:          List[(String, String)],
      maybeLinkTextHrefPairs: Option[List[(String, String)]] = None
    ): Unit = {
      val listItemsKeys   = doc.select(".govuk-summary-list__key").iterator().asScala.toList
      val listItemsValues = doc.select(".govuk-summary-list__value").iterator().asScala.toList

      for (i <- keyValuePairs.indices) {
        listItemsKeys(i).text() shouldBe keyValuePairs(i)._1
        listItemsValues(i).text() shouldBe keyValuePairs(i)._2
      }

      maybeLinkTextHrefPairs foreach { linkTextHrefPairs =>
        linkTextHrefPairs.foreach { case (text: String, href: String) =>
          doc
            .select("a")
            .select(".govuk-link")
            .select(s"""[href="$href"]""")
            .text() should include(text)
        }
      }
      ()
    }

    def checkHasTabs(tabItems: List[String]): Unit = {
      val tabItemContent = doc.select(".govuk-tabs__tab").iterator().asScala.toList
      for (i <- tabItems.indices)
        tabItemContent(i).text() shouldBe tabItems(i)
    }

    def checkHasTable(columnHeaders: List[String], rowHeaders: List[String], cells: List[String]): Unit = {

      val tableColHeaderContent =
        doc.select("""[scope="col"]""").select(".govuk-table__header").iterator().asScala.toList
      for (i <- columnHeaders.indices)
        tableColHeaderContent(i).text() shouldBe columnHeaders(i)

      val tableRowHeaderContent = doc.select("""[scope="row"]""").select(".govuk-table__cell").iterator().asScala.toList

      for (i <- rowHeaders.indices)
        tableRowHeaderContent(i).text() shouldBe rowHeaders(i)

      val tableCellContent = doc.select(".govuk-table__cell").iterator().asScala.toList
      for (i <- cells.indices)
        tableCellContent(i).text() should include(cells(i))
    }

  }
}
