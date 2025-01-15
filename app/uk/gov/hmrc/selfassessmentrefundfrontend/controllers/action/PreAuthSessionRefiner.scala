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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action

import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Request, Result}
import uk.gov.hmrc.selfassessmentrefundfrontend.config.AppConfig
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.JourneyConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.RequestSupport.hc
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.PreAuthRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreAuthSessionRefiner @Inject() (
    appConfig:        AppConfig,
    journeyConnector: JourneyConnector
)(implicit ec: ExecutionContext) extends ActionRefiner[Request, PreAuthRequest] with Logging {

  override protected def refine[A](request: Request[A]): Future[Either[Result, PreAuthRequest[A]]] = {
    implicit val r: Request[A] = request

    hc(request).sessionId match {
      case Some(sessionId) => {
        journeyConnector.findLatestBySessionId().map { journey =>
          Right(new PreAuthRequest(
            request   = request,
            journey   = journey,
            sessionId = sessionId
          ))
        }.recover {
          case err =>
            logger.warn(s"PreAuthSessionRefiner: Exception: ${err.getMessage}")
            throw err
        }
      }
      case None =>
        // TODO fix "appConfig.authLoginStubUrl" ?
        logger.warn(s"PreAuthSessionRefiner: Expected SessionId for logged in user")
        Future.successful(Left(Redirect(appConfig.authLoginStubUrl)))
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
