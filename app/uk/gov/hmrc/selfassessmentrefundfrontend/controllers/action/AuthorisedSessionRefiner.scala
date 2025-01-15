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
import play.api.mvc.{ActionRefiner, Request, Result}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.AuditService
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.IVUpliftConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.RequestSupport.hc
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.{AuthenticatedRequest, PreAuthRequest}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.MtdItId
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.JourneyTypes.TrackJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{JourneyType, JourneyTypes}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthorisedSessionRefiner @Inject() (
    val authConnector: AuthConnector,
    auditService:      AuditService,
    ivUpliftConnector: IVUpliftConnector
)(implicit ec: ExecutionContext) extends ActionRefiner[PreAuthRequest, AuthenticatedRequest] with AuthorisedFunctions with Logging {

  override protected def refine[A](request: PreAuthRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val r: Request[A] = request.request

    authorised(AuthorisedSessionRefiner.defaultAuthPredicate(request.journey.nino, request.journey.mtdItId))
      .retrieve(Retrievals.affinityGroup and Retrievals.confidenceLevel) {
        case affinityGroup ~ confidenceLevel =>
          affinityGroup match {
            case Some(AffinityGroup.Agent) =>
              authenticate(request, affinityGroup)
            case Some(AffinityGroup.Individual) if confidenceLevel.level < 250 =>
              sendFailureAuditEvent(request.journey.journeyType, request.journey.nino, Some(AffinityGroup.Individual), "low confidence level")
              ivUpliftConnector.performUplift(request, "Individual").map(Left(_))
            case Some(AffinityGroup.Organisation) if confidenceLevel.level < 250 =>
              sendFailureAuditEvent(request.journey.journeyType, request.journey.nino, Some(AffinityGroup.Organisation), "low confidence level")
              ivUpliftConnector.performUplift(request, "Organisation").map(Left(_))
            case Some(AffinityGroup.Individual) | Some(AffinityGroup.Organisation) =>
              sendIVOutcomeEvent(request.journey.nino, affinityGroup) //this will be triggered if IV journeyId is present in URL
              authenticate(request, affinityGroup)
            case None => sys.error("No Affinity group found")
            case _    => throw UnsupportedAffinityGroup("Affinity group not expected")
          }
      }.recover {
        case ex: InsufficientEnrolments =>
          sendFailureAuditEvent(request.journey.journeyType, request.journey.nino, errorMessage = "no MTD-IT enrolment")
          throw ex
      }
  }

  private def authenticate[A](request: PreAuthRequest[A], affinityGroup: Option[AffinityGroup]) = {
    Future.successful(Right(new AuthenticatedRequest(
      request       = request,
      journey       = request.journey,
      sessionId     = request.sessionId,
      affinityGroup = affinityGroup.getOrElse(sys.error("AffinityGroup should never be None for this case"))
    )))
  }

  override protected def executionContext: ExecutionContext = ec

  private def sendFailureAuditEvent(journeyType: JourneyType, maybeNino: Option[customer.Nino], affinityGroup: Option[AffinityGroup] = None, errorMessage: String)(implicit request: Request[_]): Unit = {
    journeyType match {
      case JourneyTypes.RefundJourney =>
        auditService.auditRefundAmount(None, None, None, affinityGroup, maybeNino, Some(errorMessage))
      case JourneyTypes.TrackJourney =>
        auditService.auditViewRefundStatus(None, affinityGroup, maybeNino, TrackJourney, failureReason = Some(errorMessage))
    }
  }

  private def sendIVOutcomeEvent(nino: Option[customer.Nino], affinityGroup: Option[AffinityGroup])(implicit request: Request[_]): Unit = {
    val optIVJourneyId = request.getQueryString("journeyId") //continue URLs have journeyId appended when coming back from IV
    optIVJourneyId.fold(())(_ => auditService.auditIVOutcome(isSuccessful  = true, maybeNino = nino, affinityGroup = affinityGroup.map(_.toString)))
  }
}

object AuthorisedSessionRefiner {
  def defaultAuthPredicate(nino: Option[customer.Nino], mtdItId: Option[MtdItId]): Predicate =
    individualAuthPredicate(nino.map(_.value).getOrElse("")) or agentAuthPredicate(mtdItId.map(_.value).getOrElse(""))

  private def individualAuthPredicate(nino: String): Predicate =
    Nino(hasNino = true, nino = Some(nino)) or
      Enrolment("").withIdentifier("NINO", nino)

  private def agentAuthPredicate(mtdItId: String): Predicate =
    Enrolment("HMRC-MTD-IT")
      .withIdentifier("MTDITID", mtdItId)
      .withDelegatedAuthRule("mtd-it-auth")
}
