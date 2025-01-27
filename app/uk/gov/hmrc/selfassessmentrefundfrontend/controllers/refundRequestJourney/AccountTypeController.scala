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

import javax.inject.{Inject, Singleton}
import play.api.data.Forms.{mapping, of}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Mapping}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.JourneyConnector
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.action.Actions
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney
import uk.gov.hmrc.selfassessmentrefundfrontend.controllers.refundRequestJourney.AccountTypeController.{AccountTypeEnum, AccountTypeRequest, accountTypePageModel}
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.util.{Enumerable, WithName}
import uk.gov.hmrc.selfassessmentrefundfrontend.views.html.refundrequestjourney.AccountTypePage

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountTypeController @Inject() (
    mcc:              MessagesControllerComponents,
    journeyConnector: JourneyConnector,
    actions:          Actions,
    accountTypePage:  AccountTypePage
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  val getAccountType: Action[AnyContent] = actions.authenticatedRefundJourneyAction { implicit request =>
    Ok(accountTypePage(accountTypePageModel(request.journey.accountType.map(AccountTypeEnum.accountType2Enum), AccountTypeRequest.form, request.isAgent)))
  }

  val postAccountType: Action[AnyContent] = actions.authenticatedRefundJourneyAction.async { implicit request =>
    withFormData(request.isAgent) { formAccountTypeEnum =>

      val formAccountType: AccountType = AccountTypeEnum.accountEnum2Type(formAccountTypeEnum)
      val isResponseTheSame: Boolean = request.journey.accountType.contains(formAccountType)
      val isFromCyaPage = request.session.get("self-assessment-refund.changing-account-from-cya-page").contains("redirectToCYA")

      val (call: Call, bankAccountInfo: Option[BankAccountInfo]) = if (isResponseTheSame && isFromCyaPage) {
        (refundRequestJourney.routes.CheckYourAnswersPageController.start, request.journey.bankAccountInfo)
      } else {
        (refundRequestJourney.routes.BankAccountDetailsController.getAccountDetails, None)
      }

      journeyConnector.setJourney(
        request.journey.id,
        request.journey.copy(accountType     = Some(formAccountType), bankAccountInfo = bankAccountInfo)
      ).map { _ =>
          Redirect(call).removingFromSession("self-assessment-refund.changing-account-from-cya-page")
        }

    }
  }

  private def withFormData(isAgent: Boolean)(f: AccountTypeEnum => Future[Result])(implicit messages: Messages, request: Request[_]): Future[Result] = {
    AccountTypeRequest.form.bindFromRequest().fold(
      frm => Future.successful(BadRequest(accountTypePage(accountTypePageModel(None, frm, isAgent)(messages))(messages, request))),
      r => f(r.accountType)
    )
  }
}

object AccountTypeController {

  sealed trait AccountTypeEnum extends Product with Serializable

  object AccountTypeEnum extends Enumerable.Implicits {
    case object Personal extends WithName("personal") with AccountTypeEnum
    case object Business extends WithName("business") with AccountTypeEnum

    val values: Seq[AccountTypeEnum] = Seq(Personal, Business)

    def accountType2Enum(a: AccountType): AccountTypeEnum = a match {
      case AccountType.Personal => Personal
      case AccountType.Business => Business
      case other                => throw new MatchError(s"Unknown AccountType: ${other.name}")
    }

    def accountEnum2Type(a: AccountTypeEnum): AccountType = a match {
      case Personal => AccountType.Personal
      case Business => AccountType.Business
    }

    implicit val enumerable: Enumerable[AccountTypeEnum] =
      Enumerable(values.map(v => v.toString -> v): _*)
  }

  final case class AccountTypeRequest(accountType: AccountTypeEnum)

  object AccountTypeRequest {

    val form: Form[AccountTypeRequest] =
      Form(
        mapping(
          "accountType" -> accountTypeMapping
        )(AccountTypeRequest.apply)(AccountTypeRequest.unapply)
      )

    // Need to do this as if the radio buttons are not selected then we don't get the parameter at all.
    def accountTypeMapping: Mapping[AccountTypeEnum] = {
        def permissiveStringFormatter: Formatter[AccountTypeEnum] =
          new Formatter[AccountTypeEnum] {
            def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AccountTypeEnum] = {
              val accountType = data.get(key).flatMap(AccountTypeEnum.enumerable.withName)
              accountType.fold[Either[Seq[FormError], AccountTypeEnum]](Left[Seq[FormError], AccountTypeEnum](Seq(FormError("accountType", "accountType.error.required")))){ x => Right[Seq[FormError], AccountTypeEnum](x) }
            }

            def unbind(key: String, value: AccountTypeEnum): Map[String, String] = Map(key -> value.toString)
          }
      of[AccountTypeEnum](permissiveStringFormatter)
    }
  }

  final case class AccountTypePageModel(title: String, heading: String, text: String, form: Form[AccountTypeRequest], postAccountType: Call, isBusinessAccount: Boolean, isPersonalAccount: Boolean, isAgent: Boolean)

  def accountTypePageModel(accountType: Option[AccountTypeEnum], frm: Form[AccountTypeRequest], isAgent: Boolean)(implicit messages: Messages): AccountTypePageModel = {
    val postCall = routes.AccountTypeController.postAccountType
    val isBusinessAccount = accountType.contains(AccountTypeEnum.Business)
    val isPersonalAccount = accountType.contains(AccountTypeEnum.Personal)
    val title = if (frm.hasErrors) messages("accountType.error.title") else messages("accountType.title")
    val heading = messages("accountType.heading")
    val text = messages("accountType.text")
    AccountTypePageModel(title, heading, text, frm, postCall, isBusinessAccount, isPersonalAccount, isAgent)
  }

}
