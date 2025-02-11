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

package uk.gov.hmrc.selfassessmentrefundfrontend.bars

import play.api.Logging
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.AuditService
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.request.{BarsBankAccount, BarsBusiness, BarsSubject}
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.response.BarsVerifyResponse.NonStandardAccountDetailsRequired
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.response._
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.{BarsTypeOfBankAccount, BarsTypesOfBankAccount}
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.BarsVerifyStatusConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.model.{BarVerifyStatusId, BarsVerifyStatusResponse}
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.BarsVerifiedRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.model.{AccountType, BankAccountInfo}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ItsaBarsService @Inject() (
    barsService:               BarsService,
    auditService:              AuditService,
    barsVerifyStatusConnector: BarsVerifyStatusConnector
)(implicit ec: ExecutionContext) extends Logging {

  def verifyBankDetails(
      bankAccountDetails: BankAccountInfo,
      typeOfAccount:      AccountType
  )(implicit request: BarsVerifiedRequest[_]): Future[Either[BarsError, VerifyResponse]] = {

    import ItsaBarsService._

    val resp =
      barsService.verifyBankDetails(
        bankAccount       = ItsaBarsService.toBarsBankAccount(bankAccountDetails),
        subject           = toBarsSubject(bankAccountDetails),
        business          = toBarsBusiness(bankAccountDetails),
        typeOfBankAccount = toBarsTypeOfBankAccount(typeOfAccount)
      ).flatMap { result =>
            def auditBars(barsVerifyStatusResponse: BarsVerifyStatusResponse): Unit = {
              auditService.auditBarsCheck(
                bankAccountDetails,
                result,
                barsVerifyStatusResponse,
                request.journey.accountType,
                Some(request.affinityGroup),
                request.journey.nino,
                request.agentReferenceNumber
              )
            }

          result match {
            // verify success but requires extra details ("roll number")
            case result @ Right(VerifyResponse(NonStandardAccountDetailsRequired())) if bankAccountDetails.rollNumber.isEmpty =>
              auditBars(BarsVerifyStatusResponse(request.numberOfBarsVerifyAttempts, None))
              Future.successful(Left(NonStandardDetailsRequired(result.value)))
            // a verify success or validate error
            case result @ (Right(_) | Left(_: BarsValidateError) | Left(_: ThirdPartyError)) =>
              // don't update the verify count in this case
              auditBars(BarsVerifyStatusResponse(request.numberOfBarsVerifyAttempts, None))
              Future.successful(result)
            case result @ Left(bve: BarsVerifyError) =>
              updateVerifyStatus(result, bve.barsResponse, auditBars)
          }
        }

    resp
  }

  private def updateVerifyStatus(
      result:    Either[BarsError, VerifyResponse],
      br:        BarsResponse,
      auditBars: BarsVerifyStatusResponse => Unit
  )(implicit request: BarsVerifiedRequest[_]): Future[Either[BarsError, VerifyResponse]] = {
    request.journey.nino match {
      case Some(nino) =>
        barsVerifyStatusConnector
          .update(BarVerifyStatusId.from(nino))
          .map { verifyStatus =>
            auditBars(verifyStatus)
            // here we catch a lockout BarsStatus condition and force a TooManyAttempts (BarsError) response
            verifyStatus.lockoutExpiryDateTime
              .fold(result) { expiry =>
                Left(TooManyAttempts(br, expiry))
              }
          } recover {
            case e =>
              logger.error(s"[ItsaBarsService][updateVerifyStatus] updated failed for: ${request.journey.id.toString}, reason: ${e.getMessage}")
              result
          }
      case None =>
        // pretty sure this cannot happen in reality, but the code allows it!
        logger.error(s"updateVerifyStatus issue, please investigate/fix")
        logger.error(s"without a Nino we cannot store the number of BARs verify calls and so cannot lockout for journeyId: ${request.journey.id.toString}")
        Future.successful(result)
    }
  }
}

object ItsaBarsService {
  def toBarsBankAccount(bankDetails: BankAccountInfo): BarsBankAccount =
    BarsBankAccount.normalise(bankDetails.sortCode.value, bankDetails.accountNumber.value)

  def toBarsSubject(bankDetails: BankAccountInfo): BarsSubject = BarsSubject(
    title     = None,
    name      = Some(bankDetails.name),
    firstName = None,
    lastName  = None,
    dob       = None,
    address   = None
  )

  def toBarsBusiness(bankDetails: BankAccountInfo): BarsBusiness = {
    BarsBusiness(companyName = bankDetails.name, address = None)
  }

  def toBarsTypeOfBankAccount(accountType: AccountType): BarsTypeOfBankAccount =
    accountType match {
      case AccountType.Personal => BarsTypesOfBankAccount.Personal
      case AccountType.Business => BarsTypesOfBankAccount.Business
      case s                    => throw new IllegalArgumentException(s"invalid account type ${s.name}")
    }

}
