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
import play.api.data.Forms.{bigDecimal, mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormBinding, FormError, Mapping}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.ErrorSummary
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errorsummary.ErrorLink
import uk.gov.hmrc.selfassessmentrefundfrontend.model.page.SelectAmountPageModel.SelectAmountForm
import uk.gov.hmrc.selfassessmentrefundfrontend.util.AmountFormatter
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

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
      SelectAmountForm.form(availableCredit),
      suggestedRepaymentSelected,
      isAgent
    )
  }

  final case class SelectAmountForm(amount: BigDecimal)

  object SelectAmountForm {
    val min: BigDecimal = 0.01

    private def amountOutsideExpectedRange(max: BigDecimal): Constraint[BigDecimal] = {
      val error = ValidationError("selectamount.amount.error.outsideRange", AmountFormatter.formatAmount(max))

      Constraint((t: BigDecimal) => if (t <= max && t >= min) Valid else Invalid(Seq(error)))
    }

    def form(availableCredit: BigDecimal): Form[SelectAmountForm] = {
      val boundedDecimal = bigDecimal(10, 2)
        .verifying(amountOutsideExpectedRange(availableCredit))

        def amount: Mapping[BigDecimal] = optional(boundedDecimal)
          .verifying("selectamount.amount.error.empty", _.nonEmpty)
          .transform[BigDecimal](_.getOrElse(sys.error("Could not find amount")), Some(_))

        def coerce(amount: Option[BigDecimal]): SelectAmountForm =
          SelectAmountForm(amount.getOrElse(availableCredit))

        def uncoerce(data: SelectAmountForm): Option[(Some[BigDecimal], Some[String])] = {
          val different = if (data.amount =!= availableCredit) "partial" else "full"

          Option {
            (Some(data.amount), Some(different))
          }
        }

      Form(mapping(
        "amount" -> mandatoryIf(isEqual("choice", "partial"), amount),
        "choice" -> optional(text)
          .verifying("selectamount.choice.error.required", {
            _.isDefined
          })
      ){ case (amount, _) => coerce(amount) }(uncoerce))
    }
  }
}
