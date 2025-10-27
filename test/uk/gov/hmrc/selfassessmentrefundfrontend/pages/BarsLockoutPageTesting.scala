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

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, Instant, ZoneId}
import java.util.Locale

trait BarsLockoutPageTesting extends PageContentTesting {

  def checkPageContent(expiryDateTime: Instant, buttonLink: String)(doc: Document): Unit = {
    doc.checkHasNoBackLink()
    doc.checkHasParagraphs(
      List(
        "Your refund request has not been submitted.",
        s"You can try again after ${formatDateTime(expiryDateTime)} to confirm your bank details."
      )
    )

    doc.checkHasActionAsButton(
      buttonLink,
      "Go to tax account"
    )
  }

  def checkPageContentWelsh(expiryDateTime: Instant, buttonLink: String)(doc: Document): Unit = {
    doc.checkHasNoBackLink()
    doc.checkHasParagraphs(
      List(
        "Nid yw’ch cais am ad-daliad wedi’i gyflwyno.",
        s"Gallwch roi cynnig arall ar gadarnhau’ch manylion banc ar ôl ${formatDateTimeWelsh(expiryDateTime)}."
      )
    )

    doc.checkHasActionAsButton(
      buttonLink,
      "Ewch i’r cyfrif treth"
    )
  }

  private def formatDateTime(dt: Instant): String = {
    val zonedDateTime = dt.atZone(ZoneId.of("Europe/London"))
    val date          = zonedDateTime.toLocalDate
    val time          = zonedDateTime.toLocalTime
    val dayOfWeek     = date.getDayOfWeek.toString
      .split(' ')
      .map(day => day.charAt(0).toString + day.slice(1, day.length).toLowerCase(Locale.UK))
      .mkString(" ")
    val dateString    =
      s"$dayOfWeek ${date.getDayOfMonth.toString} ${date.getMonth.toString.toLowerCase(Locale.UK).capitalize} ${date.getYear.toString}"
    val timeString    = DateTimeFormatter
      .ofPattern("h:mma")
      .format(time)
      .replaceAll("AM$", "am")
      .replaceAll("PM$", "pm")

    s"$timeString on $dateString"
  }

  private val WeekDaysInWelsh = Map(
    DayOfWeek.MONDAY    -> "dydd Llun",
    DayOfWeek.TUESDAY   -> "dydd Mawrth",
    DayOfWeek.WEDNESDAY -> "dydd Mercher",
    DayOfWeek.THURSDAY  -> "dydd Iau",
    DayOfWeek.FRIDAY    -> "dydd Gwener",
    DayOfWeek.SATURDAY  -> "dydd Sadwrn",
    DayOfWeek.SUNDAY    -> "dydd Sul"
  )

  private val MonthNamesInWelsh = Map(
    1  -> "Ionawr",
    2  -> "Chwefror",
    3  -> "Mawrth",
    4  -> "Ebrill",
    5  -> "Mai",
    6  -> "Mehefin",
    7  -> "Gorffennaf",
    8  -> "Awst",
    9  -> "Medi",
    10 -> "Hydref",
    11 -> "Tachwedd",
    12 -> "Rhagfyr"
  )

  private def formatDateTimeWelsh(dt: Instant): String = {
    val zonedDateTime = dt.atZone(ZoneId.of("Europe/London"))
    val date          = zonedDateTime.toLocalDate
    val time          = zonedDateTime.toLocalTime
    val day           = WeekDaysInWelsh(date.getDayOfWeek)
    val month         = MonthNamesInWelsh(date.getMonthValue)
    val dateString    =
      s"$day ${date.getDayOfMonth.toString} ${month.toLowerCase(Locale.UK).capitalize} ${date.getYear.toString}"
    val timeString    = DateTimeFormatter
      .ofPattern("h:mma")
      .format(time)
      .replaceAll("AM$", "am")
      .replaceAll("PM$", "pm")

    s"$timeString $dateString"
  }
}
