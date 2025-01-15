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

package uk.gov.hmrc.selfassessmentrefundfrontend.util

import java.text.DecimalFormat
import java.util.{Currency, Locale}

object AmountFormatter {

  private val formatter: DecimalFormat = {
    Locale.setDefault(Locale.UK)
    val en = Currency.getInstance(Locale.UK)
    val f: DecimalFormat = new DecimalFormat("¤#,##0.00")
    f.setCurrency(en)
    f
  }

  def formatOptionalAmount(amountInPence: Option[BigDecimal]): String = {
    amountInPence match {
      case Some(amt) => formatAmount(amt)
      case None      => ""
    }
  }

  def formatAmount(amount: BigDecimal): String = {
    val strippedAmount = amount.bigDecimal.stripTrailingZeros
    formatter.format(strippedAmount).replace(".00", "")
  }
}
