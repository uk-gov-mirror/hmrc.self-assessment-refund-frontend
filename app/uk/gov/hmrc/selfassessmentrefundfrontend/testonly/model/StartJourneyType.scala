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

import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.selfassessmentrefundfrontend.model.start.StartRequest

import scala.collection.immutable

sealed trait StartJourneyType extends enumeratum.EnumEntry {
  val label: String
  val cls: Class[_ <: StartRequest]
}

object StartJourneyType extends enumeratum.Enum[StartJourneyType] with enumeratum.PlayEnum[StartJourneyType] {
  case object StartRefund extends StartJourneyType {
    override val label: String = "Start Refund"
    override val cls = classOf[StartRequest.StartRefund]
  }

  case object ViewHistory extends StartJourneyType {
    override val label: String = "View History"
    override val cls = classOf[StartRequest.ViewHistory]
  }

  def fromSsarj(request: StartRequest): StartJourneyType = request match {
    case _: StartRequest.StartRefund => StartJourneyType.StartRefund
    case _: StartRequest.ViewHistory => StartJourneyType.ViewHistory
  }

  override def values: immutable.IndexedSeq[StartJourneyType] = findValues

  implicit val binder: QueryStringBindable[StartJourneyType] = enumeratum.UrlBinders.queryBinder[StartJourneyType](StartJourneyType)
  //  implicit val binder: QueryStringBindable[StartJourneyType] = enumeratum.UrlBinders.queryBinder[StartJourneyType](StartJourneyType)
}
