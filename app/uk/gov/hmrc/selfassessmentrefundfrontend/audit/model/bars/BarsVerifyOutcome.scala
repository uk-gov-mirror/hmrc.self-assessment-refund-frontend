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

package uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.bars

import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.response._

final case class BarsVerifyOutcome(
  isBankAccountValid:    Boolean,
  unsuccessfulAttempts:  Int,
  lockoutExpiryDateTime: Option[String],
  barsResults:           Either[BarsError, VerifyResponse]
)

object BarsVerifyOutcome {

  implicit val responseWrites: OWrites[Either[BarsError, VerifyResponse]] =
    OWrites {
      case Left(e: BarsError) =>
        e.barsResponse match {
          case ValidateResponse(barsValidateResponse) => BarsValidateResponse.format.writes(barsValidateResponse)
          case VerifyResponse(barsVerifyResponse)     => BarsVerifyResponse.format.writes(barsVerifyResponse)
          case SortCodeOnDenyList(barsErrorResponse)  => BarsErrorResponse.format.writes(barsErrorResponse)
        }

      case Right(VerifyResponse(barsVerifyResponse: BarsVerifyResponse)) =>
        BarsVerifyResponse.format.writes(barsVerifyResponse)
    }

  implicit val writes: OWrites[BarsVerifyOutcome] = Json.writes

}
