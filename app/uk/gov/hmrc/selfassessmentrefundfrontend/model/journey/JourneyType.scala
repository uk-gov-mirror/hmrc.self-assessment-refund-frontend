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

package uk.gov.hmrc.selfassessmentrefundfrontend.model.journey

import enumeratum.{Enum, EnumEntry}
import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}

sealed trait JourneyType extends EnumEntry derives CanEqual

@SuppressWarnings(
  Array("org.wartremover.warts.Serializable", "org.wartremover.warts.JavaSerializable", "org.wartremover.warts.Product")
)
object JourneyType {
  implicit val format: Format[JourneyType] = Format(
    Reads {
      case JsString(value) =>
        JourneyTypes
          .withNameOption(value)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Unknown JourneyType value: $value"))
      case _               => JsError("Can only parse String")
    },
    Writes(v => JsString(v.entryName))
  )
}

object JourneyTypes extends Enum[JourneyType] {
  case object RefundJourney extends JourneyType
  case object TrackJourney  extends JourneyType

  override val values = findValues
}
