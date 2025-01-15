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

package uk.gov.hmrc.selfassessmentrefundfrontend.testonly.model

import cats.implicits.catsSyntaxOptionId
import io.lemonlabs.uri.QueryString
import play.api.data.Form
import play.api.data.Forms.{bigDecimal, mapping, optional, text}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.selfassessmentrefundfrontend.model.ReturnUrl
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest.{StartRefund, ViewHistory}
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

final case class StartJourneyOptions(
    `type`:            StartJourneyType,
    nino:              String,
    fullAmount:        Option[BigDecimal] = None,
    lastPaymentMethod: Option[String]     = None,
    primeStubs:        PrimeStubsOption   = PrimeStubsOption.IfNotExists,
    returnUrl:         Option[String]     = None
) {

  def toSsarjRequest: StartRequest = {
    `type` match {
      case StartJourneyType.StartRefund => StartRefund(
        nino, fullAmount.getOrElse(sys.error("Could not find full amount")), lastPaymentMethod.contains("CARD").some, returnUrl.map(ReturnUrl(_))
      )
      case StartJourneyType.ViewHistory => ViewHistory(
        nino
      )
    }
  }

}

object StartJourneyOptions {
  def default: StartJourneyOptions = StartJourneyPresets.default

  implicit def bindable(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[StartJourneyOptions] = new QueryStringBindable[StartJourneyOptions] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, StartJourneyOptions]] = {
      val journeyType = StartJourneyType.queryBindable.bind("type", params).collect {
        case Right(value) => value
      }.getOrElse(StartJourneyType.StartRefund)

      val nino = stringBinder.bind("nino", params).flatMap(_.toOption).getOrElse("")

      val fullAmount: Option[BigDecimal] = QueryStringBindable.bindableDouble.bind("fullAmount", params).collect {
        case Right(value) => BigDecimal.apply(value)
      }

      val lastPaymentMethod = stringBinder.bind("lastPaymentMethod", params).collect {
        case Right(v) => v
      }

      val returnUrl = stringBinder.bind("returnUrl", params).collect {
        case Right(v) => v
      }

      val primeStubs = PrimeStubsOption.queryBindable.bind("primeStubs", params).collect {
        case Right(value) => value
      }.getOrElse(PrimeStubsOption.IfNotExists)

      val details = StartJourneyOptions(
        journeyType,
        nino,
        fullAmount,
        lastPaymentMethod,
        primeStubs,
        returnUrl
      )

      Some(Right(details))
    }

    override def unbind(key: String, value: StartJourneyOptions): String = {
      val qs = QueryString.fromPairs(
        "type" -> value.`type`.entryName,
        "nino" -> value.nino,
        "fullAmount" -> value.fullAmount.map(_.toString).getOrElse(""),
        "lastPaymentMethod" -> value.lastPaymentMethod.getOrElse("BACS"),
        "primeStubs" -> value.primeStubs.entryName,
        "returnUrl" -> value.returnUrl.getOrElse("")
      )

      qs.toString()
    }
  }

  implicit val form: Form[StartJourneyOptions] = {
    Form(
      mapping(
        "type" -> StartJourneyType.formField,
        "nino" -> text.verifying("NINO required", _.nonEmpty),
        "fullAmount" -> mandatoryIf(isEqual("type", "StartRefund"), bigDecimal(10, 2)),
        //        "fullAmount" -> optional(bigDecimal(10, 2)),
        "lastPaymentMethod" -> optional(text),
        "primeStubs" -> PrimeStubsOption.formField,
        "returnUrl" -> optional(text)
      )(StartJourneyOptions.apply)(StartJourneyOptions.unapply)
    )
  }

  def fromRequest(request: StartRequest): StartJourneyOptions = request match {
    case StartRefund(nino, fullAmount, lastPaymentViaCard, returnUrl) =>
      val lastPaymentMethod = if (lastPaymentViaCard.contains(true)) "CARD" else "BACS"
      StartJourneyOptions(StartJourneyType.StartRefund, nino, Some(fullAmount), lastPaymentMethod.some, returnUrl = returnUrl.map(_.value))
    case ViewHistory(nino) => StartJourneyOptions(StartJourneyType.ViewHistory, nino)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[StartJourneyOptions] = Json.format[StartJourneyOptions]
}
