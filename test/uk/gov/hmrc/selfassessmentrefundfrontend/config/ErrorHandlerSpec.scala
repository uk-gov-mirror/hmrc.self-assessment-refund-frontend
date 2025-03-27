/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentrefundfrontend.config

import org.jsoup.Jsoup
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import support.{ItSpec, PageContentTesting}
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll

class ErrorHandlerSpec extends ItSpec with PageContentTesting {
  val errorHandler: ErrorHandler = fakeApplication().injector.instanceOf[ErrorHandler]

  "should display internal server error page in english" in {
    val result = await(errorHandler.internalServerErrorTemplate(TdAll.request))

    val doc = Jsoup.parse(result.body)

    doc.checkPageTitle("Sorry, there is a problem with the service - Request a Self Assessment refund", welsh = false)
    doc.checkPageHeading("Sorry, there is a problem with the service")
    doc.checkHasParagraphs(List(
      "Try again later.",
      "Contact HMRC if you need to speak to someone about your Self Assessment refund."
    ))
    doc.checkHasHyperlink("Contact HMRC", "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment")
  }

  "should display internal server error page in welsh" in {
    val result = await(errorHandler.internalServerErrorTemplate(TdAll.welshRequest))

    val doc = Jsoup.parse(result.body)

    doc.checkPageTitle("Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth - Gwneud cais am ad-daliad Hunanasesiad", welsh = true)
    doc.checkPageHeading("Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth")
    doc.checkHasParagraphs(List(
      "Rhowch gynnig arall arni yn nes ymlaen.",
      "Cysylltwch â CThEF os oes angen i chi siarad â rhywun am eich ad-daliad Hunanasesiad."
    ))
    doc.checkHasHyperlink("Cysylltwch â CThEF", "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/welsh-language-helplines")
  }
}
