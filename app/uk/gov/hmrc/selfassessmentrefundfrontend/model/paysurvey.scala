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

package uk.gov.hmrc.selfassessmentrefundfrontend.model

import play.api.libs.functional.syntax.toInvariantFunctorOps
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.ContentOptions.BannerTitle

final case class SsjJourneyRequest(
    origin:         String,
    returnMsg:      String,
    returnHref:     String,
    auditName:      String,
    audit:          AuditOptions,
    contentOptions: ContentOptions
)

object SsjJourneyRequest {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[SsjJourneyRequest] = Json.format[SsjJourneyRequest]

}

final case class SsjResponse(
    journeyId: PaysurvJourneyId,
    nextUrl:   String
)

object SsjResponse {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: Format[SsjResponse] = Json.format

}

final case class PaysurvJourneyId(value: String)

object PaysurvJourneyId {
  implicit val format: Format[PaysurvJourneyId] = implicitly[Format[String]].inmap(PaysurvJourneyId(_), _.value)
}

final case class AuditOptions(
    userType:  String,
    journey:   Option[String] = None,
    orderId:   Option[String] = None,
    liability: Option[String] = None
)

object AuditOptions {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: Format[AuditOptions] = Json.format[AuditOptions]

}

final case class ContentOptions(
    isWelshSupported: Boolean,
    title:            BannerTitle
)

object ContentOptions {
  final case class BannerTitle(englishValue: String, welshValue: Option[String] = None)

  object BannerTitle {

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    implicit val format: OFormat[BannerTitle] = Json.format[BannerTitle]

  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[ContentOptions] = Json.format
}
