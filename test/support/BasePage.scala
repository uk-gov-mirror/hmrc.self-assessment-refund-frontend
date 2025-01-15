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

import org.openqa.selenium.WebDriver
import org.scalatest.compatible.Assertion
import org.scalatestplus.selenium.WebBrowser
import uk.gov.hmrc.hmrcfrontend.views.Aliases.{En, Language}

import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.util.concurrent.atomic.AtomicInteger

abstract class BasePage(baseUrl: BaseUrl)(implicit driver: WebDriver) extends RichMatchers {
  import WebBrowser._

  def path: String

  def assertPageIsDisplayed(implicit lang: Language = En): Unit

  def assertErrors(errors: (String, String)*): Unit = {
    assertPath()

    errors.foreach {
      case (summaryErrorId, text) =>
        id(summaryErrorId).element.text shouldBe text
    }
  }

  def assertNoErrors(): Assertion = {
    assertPath()

    className("govuk-error-summary").findElement shouldBe None
  }

  def assertAccountOnFile(): Assertion = {
    currentPath shouldBe "/self-assessment-refund/we-need-to-get-your-bank-details"
  }

  def assertMovedToAccountOnFileContinue(): Boolean = {
    currentPath.contains("/bank-account/account-type/")
  }

  def open(): Unit = {
    driver.get(s"${baseUrl.url}$path")
  }

  def currentPath: String = {
    val url = new java.net.URI(driver.getCurrentUrl).toURL
    url.getPath
  }

  def assertPath(): Assertion = {
    currentPath shouldBe path
  }

  def clickContinue(): Unit = {
    val button = id("continue")

    click on button
  }

}

object BasePage {
  private val time = now()
  private val seq = new AtomicInteger(0)
  def nextSeq(): Int = seq.getAndIncrement()

  val dumpTargetDir: String = {
    val addon: String = BasePage.time.format(ISO_LOCAL_DATE_TIME)
    s"target/pagespec-screenshots-$addon"
  }
}
