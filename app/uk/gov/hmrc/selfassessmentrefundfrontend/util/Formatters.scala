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

package uk.gov.hmrc.selfassessmentrefundfrontend.util

import play.api.i18n.Messages
import cats.syntax.eq._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, Instant, ZoneId}
import java.util.Locale

object Formatters {

  private val WeekDaysInWelsh = Map(
    DayOfWeek.MONDAY    -> "dydd Llun",
    DayOfWeek.TUESDAY   -> "dydd Mawrth",
    DayOfWeek.WEDNESDAY -> "dydd Mercher",
    DayOfWeek.THURSDAY  -> "dydd Iau",
    DayOfWeek.FRIDAY    -> "dydd Gwener",
    DayOfWeek.SATURDAY  -> "dydd Sadwrn",
    DayOfWeek.SUNDAY    -> "dydd Sul"
  )

  def fullDateTime(dt: Instant, messages: Messages, languageUtils: LanguageUtils)(implicit
    requestHeader: RequestHeader
  ): String = {
    val zonedDateTime = dt.atZone(ZoneId.of("Europe/London"))
    val date          = zonedDateTime.toLocalDate
    val time          = zonedDateTime.toLocalTime
    val lang          = languageUtils.getCurrentLang(requestHeader).code

    val day        = lang match {
      case "cy" => WeekDaysInWelsh(date.getDayOfWeek)
      case _    =>
        date.getDayOfWeek.toString
          .split(' ')
          .map(day => day.charAt(0).toString + day.slice(1, day.length).toLowerCase(Locale.UK))
          .mkString(" ")
    }
    val dateString = s"$day ${date.getDayOfMonth.toString} ${messages(date.getMonth.toString)} ${date.getYear.toString}"
    val timeString = DateTimeFormatter
      .ofPattern("h:mma")
      .format(time)
      .replaceAll("AM$", "am")
      .replaceAll("PM$", "pm")

    if (lang === "en") {
      s"<strong>$timeString on $dateString</strong>"
    } else s"<strong>$timeString $dateString</strong>"
  }

}
