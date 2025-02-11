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

package support.stubbing

object BarsJsonResponses {

  object ValidateJson {
    val success: String =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeSupportsDirectDebit": "yes",
        |  "sortCodeSupportsDirectCredit": "yes",
        |  "iban": "GB21BARC20710244344655",
        |  "sortCodeBankName": "BARCLAYS BANK UK PLC"
        |}""".stripMargin

    val accountNumberNotWellFormatted: String =
      """{
        |  "accountNumberIsWellFormatted": "no",
        |  "nonStandardAccountDetailsRequiredForBacs": "yes",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeSupportsDirectDebit": "no",
        |  "sortCodeSupportsDirectCredit": "yes",
        |  "sortCodeBankName": "Nottingham Building Society"
        |}""".stripMargin

    val sortCodeNotPresentOnEiscd: String =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "sortCodeIsPresentOnEISCD": "no"
        |}""".stripMargin

    val sortCodeDoesNotSupportsDirectDebit: String =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeSupportsDirectDebit": "no",
        |  "sortCodeSupportsDirectCredit": "no",
        |  "iban": "GB21BARC20670544311611",
        |  "sortCodeBankName": "BARCLAYS BANK UK PLC"
        |}""".stripMargin

    val sortCodeOnDenyList: String =
      """{
        |  "code": "SORT_CODE_ON_DENY_LIST",
        |  "desc": "q"
        |}""".stripMargin
  }

  object VerifyJson {
    val success: String =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "yes",
        |    "nameMatches": "yes",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "yes",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    def thirdPartyError(accountExists: String, nameMatches: String): String =
      s"""{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "$accountExists",
        |    "nameMatches": "$nameMatches",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "yes",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val accountNumberNotWellFormatted =
      """{
        |    "accountNumberIsWellFormatted": "no",
        |    "accountExists": "yes",
        |    "nameMatches": "yes",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "yes",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val sortCodeDoesNotSupportDirectCredit =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "yes",
        |    "nameMatches": "yes",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "no",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val sortCodeNotPresentOnEiscd =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "yes",
        |    "nameMatches": "yes",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "no",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "yes",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val nonStandardDetailsRequired =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "yes",
        |    "nameMatches": "yes",
        |    "nonStandardAccountDetailsRequiredForBacs": "yes",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "yes",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val accountExistsError =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "error",
        |    "nameMatches": "indeterminate",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "no",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val accountDoesNotExist =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "no",
        |    "nameMatches": "yes",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "yes",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val nameMatchesError =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "indeterminate",
        |    "nameMatches": "error",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "yes",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val nameDoesNotMatch =
      """{
        |    "accountNumberIsWellFormatted": "yes",
        |    "accountExists": "yes",
        |    "nameMatches": "no",
        |    "nonStandardAccountDetailsRequiredForBacs": "no",
        |    "sortCodeIsPresentOnEISCD": "yes",
        |    "sortCodeBankName": "BARCLAYS BANK UK PLC",
        |    "sortCodeSupportsDirectDebit": "yes",
        |    "sortCodeSupportsDirectCredit": "yes",
        |    "iban": "GB21BARC20710244311655"
        |}""".stripMargin

    val otherBarsError =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeBankName": "BARCLAYS BANK PLC",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "accountExists": "inapplicable",
        |  "nameMatches": "inapplicable",
        |  "sortCodeSupportsDirectDebit": "indeterminate",
        |  "sortCodeSupportsDirectCredit": "indeterminate"
        |}""".stripMargin

  }

}
