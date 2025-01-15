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

import org.scalatest.wordspec.AnyWordSpec
import support.RichMatchers

class AmountFormatterSpec extends AnyWordSpec with RichMatchers {

  "formatOptionalAmount" should {
    "return the formatted amount" in {
      AmountFormatter.formatOptionalAmount(Some(1000.0000)) shouldBe "£1,000"
    }

    "return an empty string when amount is None" in {
      AmountFormatter.formatOptionalAmount(None) shouldBe ""
    }
  }

  "formatAmount" should {
    "return the formatted amount" in {
      AmountFormatter.formatAmount(BigDecimal(1000567.89)) shouldBe "£1,000,567.89"
    }

    "return the formatted amount without adding decimal places" in {
      AmountFormatter.formatAmount(BigDecimal(4000)) shouldBe "£4,000"
    }

    "return the formatted amount without decimal places" in {
      AmountFormatter.formatAmount(BigDecimal(4000.00)) shouldBe "£4,000"
    }

    "return the formatted amount to two decimal places" in {
      AmountFormatter.formatAmount(BigDecimal(4000.7)) shouldBe "£4,000.70"
    }

    "return the formatted amount, ROUNDED UP" in {
      AmountFormatter.formatAmount(BigDecimal(4000.809)) shouldBe "£4,000.81"
    }

    "return the formatted amount, ROUNDED DOWN" in {
      AmountFormatter.formatAmount(BigDecimal(4000.804)) shouldBe "£4,000.80"
    }
  }

  "sanitize" should {
    for (sample <- Seq(" 1000.00", " 1000.00 ", "1,000.00", "1 000.00", "  1, 000.00  ", "£1,000.00")) {
      s"remove the whitespaces, commas, pound signs from the amount: [$sample]" in {
        AmountFormatter.sanitize(Some(sample)) shouldBe "1000.00"
      }
    }

    "throw IllegalArgumentException if provided amount is an empty string" in {
      an[IllegalArgumentException] shouldBe thrownBy {
        AmountFormatter.sanitize(Some(""))
      }
    }

    "throw IllegalArgumentException if provided value is None" in {
      an[IllegalArgumentException] shouldBe thrownBy {
        AmountFormatter.sanitize(None)
      }
    }
  }
}
