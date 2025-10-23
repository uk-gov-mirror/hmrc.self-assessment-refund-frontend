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

package uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney

import cats.data.EitherT
import play.api.Logging
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.ItsaBarsService
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.response._
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.JourneyConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.request.AuthenticatedRequest
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.BankAccountDetailsController.{AccountPageModel, BankAccount}
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.Journey
import uk.gov.hmrc.selfassessmentrefundfrontend.util.Mapping.ConversionOps
import uk.gov.hmrc.selfassessmentrefundfrontend.util.{Mapping => CMapping}
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.BankAccountDetailsPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankAccountDetailsController @Inject() (
    mcc:              MessagesControllerComponents,
    journeyConnector: JourneyConnector,
    barsService:      ItsaBarsService,
    accountPage:      BankAccountDetailsPage,
    actions:          Actions
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with Logging {

  val getAccountDetails: Action[AnyContent] = actions.authenticatedRefundJourneyAction { implicit request =>
    val journey = request.journey
    val details = journey.bankAccountInfo

    Ok(accountPage(AccountPageModel(details, request.isAgent)))
  }

  val postAccountDetails: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>
    withFormData(request.isAgent) { (form: Form[BankAccount]) => (bankAccount: BankAccount) =>
      val bankAccountInfo = BankAccountInfo(bankAccount)
      val accountType = getJourneyAccountType(request.journey)
      if (alreadyVerifiedBankDetails(accountType, bankAccountInfo, request.journey)) {
        // if bank details haven't changed don't call BARs and don't update journey
        Future.successful(Redirect(refundRequestJourney.routes.CheckYourAnswersPageController.start))
      } else {
        barsService.verifyBankDetails(bankAccountInfo, accountType)
          .flatMap { barsResponse =>
            handleBars(barsResponse, form, bankAccountInfo)
          }
      }
    }
  }

  private def handleBars(
      resp:            Either[BarsError, VerifyResponse],
      form:            Form[BankAccount],
      bankAccountInfo: BankAccountInfo
  )(implicit messages: Messages, request: AuthenticatedRequest[AnyContent]): Future[Result] = {

    def enterBankDetailsPageWithBarsError(error: FormErrorWithFieldMessageOverrides): Future[Result] = {
      Future.successful(BadRequest(
        accountPage(AccountPageModel(
          form                  = form.withError(error.formError),
          fieldMessageOverrides = error.fieldMessageOverrides,
          request.isAgent
        )(messages))(messages, request)
      ))
    }

    def saveBankInfoAndContinue: Future[Result] = {
      journeyConnector.setJourney(request.journey.id, request.journey.copy(bankAccountInfo = Some(bankAccountInfo)))
    }.flatMap { _ =>
      Future.successful(Redirect(refundRequestJourney.routes.CheckYourAnswersPageController.start))
    }

    import BankAccountDetailsForm._
    resp.fold({
        case ThirdPartyError(resp) =>
          throw new RuntimeException(s"BARS verify third-party error. BARS response: ${resp.toString}")
        case AccountNumberNotWellFormatted(_) | AccountNumberNotWellFormattedValidateResponse(_) =>
          enterBankDetailsPageWithBarsError(accountNumberNotWellFormatted)
        case SortCodeDoesNotSupportDirectCredit(_) | SortCodeDoesNotSupportDirectCreditValidateResponse(_) =>
          enterBankDetailsPageWithBarsError(sortCodeDoesNotSupportsDirectCredit)
        case SortCodeNotPresentOnEiscd(_) | SortCodeNotPresentOnEiscdValidateResponse(_) =>
          enterBankDetailsPageWithBarsError(sortCodeNotPresentOnEiscd)
        case SortCodeOnDenyListErrorResponse(_) =>
          enterBankDetailsPageWithBarsError(sortCodeOnDenyList)
        case NameDoesNotMatch(_) =>
          enterBankDetailsPageWithBarsError(nameDoesNotMatch)
        case AccountDoesNotExist(_) =>
          enterBankDetailsPageWithBarsError(accountDoesNotExist)
        case OtherBarsError(_) =>
          enterBankDetailsPageWithBarsError(otherBarsError)
        case NonStandardDetailsRequired(_) =>
          enterBankDetailsPageWithBarsError(nonStandardDetailsRequired)
        case TooManyAttempts(_, _) =>
          Future.successful(Redirect(controllers.refundRequestJourney.routes.BarsLockoutController.barsLockout))
      },
      _ =>
        saveBankInfoAndContinue
    )
  }

  private def getJourneyAccountType(journey: Journey): AccountType = {
    journey.accountType.getOrElse(throw new IllegalStateException("Missing Account Type"))
  }

  private def alreadyVerifiedBankDetails(accountType: AccountType, bankAccountInfo: BankAccountInfo, journey: Journey): Boolean = {

    journey.bankAccountInfo match {
      case Some(journeyBankAccountInfo) =>
        BankDetails(
          journey.accountType.getOrElse(sys.error("cannot find account type")).toString,
          journeyBankAccountInfo.name,
          journeyBankAccountInfo.sortCode.toString,
          journeyBankAccountInfo.accountNumber.toString,
          journeyBankAccountInfo.rollNumber.map(_.value)
        )
          .matches(accountType, bankAccountInfo)

      case None => false
    }
  }

  private def withFormData(isAgent: Boolean)(f: Form[BankAccount] => BankAccount => Future[Result])(implicit messages: Messages, request: Request[_]): Future[Result] = {
    val fromRequest = BankAccountDetailsForm.form.bindFromRequest()
    fromRequest.fold(
      form => Future.successful(BadRequest(accountPage(AccountPageModel(form, Seq.empty, isAgent)(messages))(messages, request))),
      f(fromRequest)
    )
  }
}

object BankAccountDetailsController {

  type ApiResult = EitherT[Future, ApiError, Result]

  sealed trait ApiError
  case object MissingNino extends ApiError
  case object MissingAccountType extends ApiError

  def missingAccountType: ApiError = MissingAccountType

  def missingNino: ApiError = MissingNino

  final case class BankAccount(accountName: String, sortCode: String, accountNumber: String, rollNumber: Option[String])

  object BankAccount {

    def unapply(bankAccount: BankAccount): Option[(String, String, String, Option[String])] =
      Some(
        (
          bankAccount.accountName,
          bankAccount.sortCode,
          bankAccount.accountNumber,
          bankAccount.rollNumber
        )
      )

    given OFormat[BankAccount] = Json.format[BankAccount]
  }

  final case class AccountPageModel(
      title:                 String,
      form:                  Form[BankAccount],
      fieldMessageOverrides: Seq[FormError]    = Seq.empty,
      postAccountDetails:    Call,
      isAgent:               Boolean
  )

  object AccountPageModel {

    val InvalidAccountKey = "bars.error.invalidAccountDetails"
    val NonExistentAccountKey = "bars.error.accountNumber.nonExistentAccount"
    val NonExistentNameKey = "bars.error.accountName.doesNotExist"
    val DirectCreditNotSupportedKey = "bars.error.sortCode.directCreditNotSupported"

    def pageTitle(form: Form[BankAccount])(implicit messages: Messages): String =
      if (form.hasErrors) messages("enter-bank-details.error.title") else messages("enter-bank-details.title")

    def apply(bankDetails: Option[BankAccountInfo], isAgent: Boolean)(implicit messages: Messages): AccountPageModel = {
      val form = bankDetails.map(i => BankAccountDetailsForm.form.fill(i.mapTo[BankAccount])).getOrElse(BankAccountDetailsForm.form)

      new AccountPageModel(
        pageTitle(form),
        form, postAccountDetails = refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails,
        isAgent            = isAgent
      )
    }

    def apply(form: Form[BankAccount], fieldMessageOverrides: Seq[FormError], isAgent: Boolean)(implicit messages: Messages): AccountPageModel = {
      new AccountPageModel(
        pageTitle(form),
        form,
        fieldMessageOverrides = fieldMessageOverrides,
        postAccountDetails    = refundRequestJourney.routes.BankAccountDetailsController.postAccountDetails,
        isAgent               = isAgent
      )
    }
  }

  implicit val conv: CMapping[BankAccount, BankAccountInfo] =
    (a: BankAccount) => BankAccountInfo(a.accountName, SortCode(a.sortCode), AccountNumber(a.accountNumber), a.rollNumber.map(RollNumber.apply))

  implicit val conv2: CMapping[BankAccountInfo, BankAccount] =
    (a: BankAccountInfo) => BankAccount(a.name, a.sortCode.value, a.accountNumber.value, a.rollNumber.map(_.value))

}
