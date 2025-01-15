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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action

import play.api.Logging
import play.api.mvc.{ActionRefiner, Request, Result, Results}
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.BarsVerifyStatusConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.model.BarVerifyStatusId
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.{AuthenticatedRequest, LockedOutJourneyRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BarsLockedOutJourneyActionRefiner @Inject() (
    barsVerifyStatusConnector: BarsVerifyStatusConnector
)(implicit ec: ExecutionContext) extends ActionRefiner[AuthenticatedRequest, LockedOutJourneyRequest] with Logging with Results {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, LockedOutJourneyRequest[A]]] = {
    implicit val rh: Request[A] = request.request

    request.journey.nino match {
      case Some(nino) =>
        barsVerifyStatusConnector.status(BarVerifyStatusId.from(nino)).map { status =>
          status.lockoutExpiryDateTime match {
            case Some(expiry) =>
              Right(new LockedOutJourneyRequest(
                request                    = request,
                journey                    = request.journey,
                sessionId                  = request.sessionId,
                barsLockoutExpiryTime      = expiry,
                numberOfBarsVerifyAttempts = status.attempts,
                returnUrl                  = request.journey.returnUrl
              ))
            case None => throw new RuntimeException("Unexpected condition. This refiner should only be called when service is locked out")
          }
        } recover {
          case e =>
            logger.error(s"[BarsLockedOutJourneyActionRefiner] failed to retrieve BarsVerifyStatus for nino=${request.journey.nino.toString}, reason:${e.getMessage}")
            Left(InternalServerError)
        }
      case None => throw new RuntimeException("Unexpected condition. This refiner should only be called when service is locked out")
    }
  }

  override protected def executionContext: ExecutionContext = ec
}

