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

package uk.gov.hmrc.selfassessmentrefundfrontend.testonly.services

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.JourneyConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.testonly.connectors.StubsAdminConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.testonly.model.{PrimeStubsOption, StartJourneyOptions}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
final case class StartJourneyService @Inject() (
    stubsAdmin:       StubsAdminConnector,
    journeyConnector: JourneyConnector
)(implicit ec: ExecutionContext) extends Logging {

  def start(
      options: StartJourneyOptions,
      origin:  String
  )(implicit hc: HeaderCarrier): Future[Result] = {
    val req = options.toSsarjRequest

    for {
      _ <- options.primeStubs match {
        case PrimeStubsOption.IfNotExists =>
          logger.info(s"Priming ${req.nino} with empty account (if not exists)")
          stubsAdmin.createAccount(req.nino, true)
        case PrimeStubsOption.SetEmpty =>
          logger.info(s"Priming ${req.nino} with empty account (reset if exists)")
          stubsAdmin.createAccount(req.nino)
        case PrimeStubsOption.SetDefault =>
          logger.info(s"Priming ${req.nino} with mock account")
          stubsAdmin.updateAccount(req.nino, mock = true)
      }
      created <- journeyConnector.start(req, origin)
    } yield Redirect(created.nextUrl)
  }

}
