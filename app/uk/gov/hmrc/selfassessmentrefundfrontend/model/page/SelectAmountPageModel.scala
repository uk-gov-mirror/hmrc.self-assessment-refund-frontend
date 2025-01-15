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

package uk.gov.hmrc.selfassessmentrefundfrontend.model.page

import cats.syntax.eq._
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormBinding, FormError, Mapping}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.ErrorSummary
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errorsummary.ErrorLink
import uk.gov.hmrc.selfassessmentrefundfrontend.model.SelectAmountChoice
import uk.gov.hmrc.selfassessmentrefundfrontend.model.SelectAmountChoice.{Full, Partial, Suggested}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.SelectAmountPageModel.SelectAmountForm
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

import scala.util.Try

final case class SelectAmountPageModel(
    selectAmount:               Option[String],
    partialRepaymentSelected:   Option[Boolean],
    availableCredit:            String,
    suggestedAmount:            Option[String],
    form:                       Form[SelectAmountForm],
    suggestedRepaymentSelected: Option[Boolean],
    isAgent:                    Boolean
) {

  def withFormBound()(implicit request: play.api.mvc.Request[_], formBinding: FormBinding): SelectAmountPageModel = copy(
    form = form.bindFromRequest()
  )

  def errorSummary(implicit messages: Messages): Option[ErrorSummary] = if (form.hasErrors) {
    val errorLinks = form.errors
      .map { formError =>
        val href = {
          val inputId = {
            formError.key match {
              case "amount" => "different-amount"
              case "choice" => if (suggestedAmount.isEmpty) {
                "choice-full"
              } else {
                "choice-suggested"
              }
            }
          }
          s"#$inputId"
        }

        val attributes = Map(
          "id" -> s"${formError.key}-error-summary"
        )

        ErrorLink(
          href       = Some(href),
          content    = Text.apply(formError.format),
          attributes = attributes
        )
      }

    Option(ErrorSummary(
      errorList = errorLinks,
      title     = HtmlContent(s"""<span id="error-summary-title">${messages("selectamount.error-summary.title")}</span>""")
    ))
  } else Option.empty

  def errorMessage(formError: FormError)(implicit messages: Messages): String = messages(formError.message, formError.args: _*)
}

object SelectAmountPageModel {

  def apply(
      chosenAmount:               Option[BigDecimal] = None,
      partialRepaymentSelected:   Option[Boolean]    = None,
      availableCredit:            BigDecimal,
      suggestedAmount:            Option[BigDecimal],
      suggestedRepaymentSelected: Option[Boolean]    = None,
      isAgent:                    Boolean
  ): SelectAmountPageModel = {
    SelectAmountPageModel(
      chosenAmount.map(_.toString()),
      partialRepaymentSelected,
      AmountFormatter.formatAmount(availableCredit),
      suggestedAmount.map(AmountFormatter.formatAmount),
      SelectAmountForm.form(availableCredit, suggestedAmount),
      suggestedRepaymentSelected,
      isAgent
    )
  }

  final case class SelectAmountForm(amount: BigDecimal, choice: SelectAmountChoice)

  @SuppressWarnings(Array(
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Product"
  ))
  object SelectAmountForm {
    val min: BigDecimal = 0.01

    private def amountOutsideExpectedRange(max: BigDecimal): Constraint[BigDecimal] = {
      val error = ValidationError("selectamount.amount.error.outsideRange", AmountFormatter.formatAmount(max))

      Constraint { (amount: BigDecimal) =>
        if (amount >= min && amount <= max) Valid
        else
          Invalid(Seq(error))
      }
    }

    def form(availableCredit: BigDecimal, suggestedAmount: Option[BigDecimal]): Form[SelectAmountForm] = {
      val amountMapping: Mapping[BigDecimal] = text
        .transform[String](chosenAmount => AmountFormatter.sanitize(Some(chosenAmount)), identity)
        .verifying("selectamount.amount.error.invalid", str =>
          Try(BigDecimal(str)).toOption.exists(amount => amount.scale <= 2 && amount.precision <= 10))
        .transform[BigDecimal](BigDecimal(_), _.toString())
        .verifying(amountOutsideExpectedRange(availableCredit))

        def amount: Mapping[BigDecimal] = optional(amountMapping)
          .verifying("selectamount.amount.error.empty", _.nonEmpty)
          .transform[BigDecimal](_.getOrElse(throw new IllegalArgumentException("[SelectAmountForm][amount] Amount is missing")), Some(_))

        def apply(choice: Option[SelectAmountChoice], amount: Option[BigDecimal]): SelectAmountForm = {
          choice match {
            case Some(Full)      => SelectAmountForm(availableCredit, Full)
            case Some(Suggested) => SelectAmountForm(suggestedAmount.getOrElse(availableCredit), Suggested)
            case Some(Partial)   => SelectAmountForm(amount.getOrElse(availableCredit), Partial)
            case other           => throw new IllegalArgumentException(s"[SelectAmountForm][apply] Expected SelectAmountChoice, got: ${other.toString}")
          }
        }

        def unapply(data: SelectAmountForm): Option[(Option[SelectAmountChoice], Option[BigDecimal])] = {
          val different = if (data.amount =!= availableCredit) Partial else Full

          Some((Some(different), Some(data.amount)))
        }

      Form(mapping(
        "choice" -> optional(text)
          .verifying("selectamount.choice.error.required", { _.isDefined })
          .transform[Option[SelectAmountChoice]](choice => choice.map(SelectAmountChoice.withNameLowercaseOnly), choice => choice.map(_.toString)),
        "amount" -> mandatoryIf(isEqual("choice", "partial"), amount)
      )(apply)(unapply))
    }
  }
}
