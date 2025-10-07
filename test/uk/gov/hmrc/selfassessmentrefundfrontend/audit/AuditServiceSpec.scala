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

package uk.gov.hmrc.selfassessmentrefundfrontend.audit

import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.scaled
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.libs.json.{Format, JsObject, JsValue, Json}
import support.ItSpec
import support.stubbing.{AuditStub, AuthStub}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, AuditResult, DatastreamMetrics}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.AuditFlags
import uk.gov.hmrc.selfassessmentrefundfrontend.bars.model.response._
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.model.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{Journey, JourneyId, JourneyTypes}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.RequestNumber
import uk.gov.hmrc.selfassessmentrefundfrontend.services.RepaymentsService._
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll.request
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdBars
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdBars.bankAccountInfo
import uk.gov.hmrc.selfassessmentrefundfrontend.util.ApplicationLogging

import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZonedDateTime, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends ItSpec with ApplicationLogging {

  def testBarsCheck(expectedDetails: JsValue)(event: ExtendedDataEvent): Any =
    testCallback(expectedDetails)(event)("BARSCheck")

  def testViewRefundStatusChecks(expectedDetails: JsValue)(event: ExtendedDataEvent): Any =
    testCallback(expectedDetails)(event)("ViewRefundStatus")

  def testRefundAmount(expectedDetails: JsValue)(event: ExtendedDataEvent): Any =
    testCallback(expectedDetails)(event)("RefundAmount")

  def testIVOutcome(expectedDetails: JsValue)(event: ExtendedDataEvent): Any =
    testCallback(expectedDetails)(event)("IdentityVerificationOutcome")

  def testCallback(expectedDetails: JsValue)(event: ExtendedDataEvent)(auditType: String): Any = {
    event.auditSource shouldBe "self-assessment-refund-frontend"

    event.auditType shouldBe auditType

    event.detail shouldBe expectedDetails
  }

  "audit Bars check" when {
    "result is a verify response" in {
      val expectedDetails: JsValue = Json.parse(
        s"""{
           |  "nino": "AA111111A",
           |  "userEnteredDetails": {
           |    "accountType": "personal",
           |    "accountHolderName": "Account Name",
           |    "sortCode": "123456",
           |    "accountNumber": "12345678"
           |  },
           |  "outcome": {
           |    "isBankAccountValid": true,
           |    "unsuccessfulAttempts": 0,
           |    "barsResults": {
           |      "accountNumberIsWellFormatted": "yes",
           |      "accountExists": "yes",
           |      "nameMatches": "yes",
           |      "nonStandardAccountDetailsRequiredForBacs": "no",
           |      "sortCodeIsPresentOnEISCD": "yes",
           |      "sortCodeSupportsDirectDebit": "yes",
           |      "sortCodeSupportsDirectCredit": "yes",
           |      "accountName": "Account Name",
           |      "sortCodeBankName": "Lloyds",
           |      "iban": "GB59 HBUK 1234 5678"
           |    }
           |  },
           |  "userType": "Individual"
           |}""".stripMargin
      ).as[JsValue]

      val auditService = new AuditService(StubAuditConnector(expectedDetails)(testBarsCheck))

      auditService.auditBarsCheck(
        BankAccountInfo(bankAccountInfo),
        Right(VerifyResponse(TdBars.barsVerifyResponseAllFields)),
        BarsVerifyStatusResponse(
          NumberOfBarsVerifyAttempts(0),
          None
        ),
        Some(AccountType("personal")),
        Some(AffinityGroup.Individual),
        Some(Nino("AA111111A")),
        None
      )(request)
    }
    "result is a verify response with 3 unsuccessfulAttempts" in {
      val expectedDetails: JsValue = Json.parse(
        s"""{
           |  "nino": "AA111111A",
           |  "userEnteredDetails": {
           |    "accountType": "personal",
           |    "accountHolderName": "Account Name",
           |    "sortCode": "123456",
           |    "accountNumber": "12345678"
           |  },
           |  "outcome": {
           |    "isBankAccountValid": true,
           |    "unsuccessfulAttempts": 3,
           |    "lockoutExpiryDateTime": "2022-09-07T10:01:15.315Z",
           |    "barsResults": {
           |      "accountNumberIsWellFormatted": "yes",
           |      "accountExists": "yes",
           |      "nameMatches": "yes",
           |      "nonStandardAccountDetailsRequiredForBacs": "no",
           |      "sortCodeIsPresentOnEISCD": "yes",
           |      "sortCodeSupportsDirectDebit": "yes",
           |      "sortCodeSupportsDirectCredit": "yes",
           |      "accountName": "Account Name",
           |      "sortCodeBankName": "Lloyds",
           |      "iban": "GB59 HBUK 1234 5678"
           |    }
           |  },
           |  "userType": "Agent",
           |  "agentReferenceNumber":"AARN1234567"
           |}""".stripMargin
      ).as[JsValue]

      val auditService = new AuditService(StubAuditConnector(expectedDetails)(testBarsCheck))

      auditService.auditBarsCheck(
        BankAccountInfo(bankAccountInfo),
        Right(VerifyResponse(TdBars.barsVerifyResponseAllFields)),
        BarsVerifyStatusResponse(
          NumberOfBarsVerifyAttempts(3),
          Some(ZonedDateTime.parse("2022-09-07T10:01:15.315Z").toInstant)
        ),
        Some(AccountType("personal")),
        Some(AffinityGroup.Agent),
        Some(Nino("AA111111A")),
        Some("AARN1234567")
      )(request)
    }
    "result is a bars error" when {
      "error is from Validate check" in {
        val expectedDetails: JsValue = Json.parse(
          s"""{
             |  "nino": "AA111111A",
             |  "userEnteredDetails": {
             |    "accountType": "personal",
             |    "accountHolderName": "Account Name",
             |    "sortCode": "123456",
             |    "accountNumber": "12345678"
             |  },
             |  "outcome": {
             |    "isBankAccountValid": false,
             |    "unsuccessfulAttempts": 3,
             |    "lockoutExpiryDateTime": "2022-09-07T10:01:15.315Z",
             |    "barsResults": {
             |      "accountNumberIsWellFormatted": "no",
             |      "nonStandardAccountDetailsRequiredForBacs": "no",
             |      "sortCodeIsPresentOnEISCD": "yes",
             |      "sortCodeSupportsDirectDebit": "yes"
             |    }
             |  },
             |  "userType": "Individual"
             |}""".stripMargin
        ).as[JsValue]

        val auditService = new AuditService(StubAuditConnector(expectedDetails)(testBarsCheck))

        auditService.auditBarsCheck(
          BankAccountInfo(bankAccountInfo),
          Left(AccountNumberNotWellFormattedValidateResponse(ValidateResponse(TdBars.barsValidateResponseAccountNumNotWellFormatted))),
          BarsVerifyStatusResponse(
            NumberOfBarsVerifyAttempts(3),
            Some(ZonedDateTime.parse("2022-09-07T10:01:15.315Z").toInstant)
          ),
          Some(AccountType("personal")),
          Some(AffinityGroup.Individual),
          Some(Nino("AA111111A")),
          None
        )(request)

      }
      "error is from Verify check" in {
        val expectedDetails: JsValue = Json.parse(
          s"""{
             |  "nino": "AA111111A",
             |  "userEnteredDetails": {
             |    "accountType": "personal",
             |    "accountHolderName": "Account Name",
             |    "sortCode": "123456",
             |    "accountNumber": "12345678"
             |  },
             |  "outcome": {
             |    "isBankAccountValid": false,
             |    "unsuccessfulAttempts": 3,
             |    "lockoutExpiryDateTime": "2022-09-07T10:01:15.315Z",
             |    "barsResults": {
             |      "accountNumberIsWellFormatted": "yes",
             |      "accountExists": "yes",
             |      "nameMatches": "yes",
             |      "nonStandardAccountDetailsRequiredForBacs": "no",
             |      "sortCodeIsPresentOnEISCD": "yes",
             |      "sortCodeSupportsDirectDebit": "yes",
             |      "sortCodeSupportsDirectCredit": "yes"
             |    }
             |  },
             |  "userType": "Individual"
             |}""".stripMargin
        ).as[JsValue]

        val auditService = new AuditService(StubAuditConnector(expectedDetails)(testBarsCheck))

        auditService.auditBarsCheck(
          BankAccountInfo(bankAccountInfo),
          Left(AccountDoesNotExist(VerifyResponse(TdBars.barsVerifyResponse))),
          BarsVerifyStatusResponse(
            NumberOfBarsVerifyAttempts(3),
            Some(ZonedDateTime.parse("2022-09-07T10:01:15.315Z").toInstant)
          ),
          Some(AccountType("personal")),
          Some(AffinityGroup.Individual),
          Some(Nino("AA111111A")),
          None
        )(request)

      }
      "error is due to sort code being on deny list" in {
        val expectedDetails: JsValue = Json.parse(
          s"""{
             |  "nino": "AA111111A",
             |  "userEnteredDetails": {
             |    "accountType": "personal",
             |    "accountHolderName": "Account Name",
             |    "sortCode": "123456",
             |    "accountNumber": "12345678"
             |  },
             |  "outcome": {
             |    "isBankAccountValid": false,
             |    "unsuccessfulAttempts": 3,
             |    "lockoutExpiryDateTime": "2022-09-07T10:01:15.315Z",
             |    "barsResults": {
             |      "code": "code",
             |      "desc": "desc"
             |    }
             |  },
             |  "userType": "Individual"
             |}""".stripMargin
        ).as[JsValue]

        val auditService = new AuditService(StubAuditConnector(expectedDetails)(testBarsCheck))

        auditService.auditBarsCheck(
          BankAccountInfo(bankAccountInfo),
          Left(SortCodeOnDenyListErrorResponse(SortCodeOnDenyList(TdBars.barsErrorResponse))),
          BarsVerifyStatusResponse(
            NumberOfBarsVerifyAttempts(3),
            Some(ZonedDateTime.parse("2022-09-07T10:01:15.315Z").toInstant)
          ),
          Some(AccountType("personal")),
          Some(AffinityGroup.Individual),
          Some(Nino("AA111111A")),
          None
        )(request)
      }
    }

  }

  @SuppressWarnings(Array("org.wartremover.warts.AnyVal"))
  override lazy val configOverrides: Map[String, Any] = Map(
    "auditing.enabled" -> true,
    "auditing.consumer.baseUri.port" -> wireMockServer.port
  )

  class Setup(affinityGroup: AffinityGroup, confidenceLevel: ConfidenceLevel = ConfidenceLevel.L50) {

    AuthStub.allEnrolments(affinityGroup, confidenceLevel)

    val auditService: AuditService = app.injector.instanceOf[AuditService]
    val testReturnUrl: Option[ReturnUrl] = Some(ReturnUrl("/returnUrl"))
    val testBankAccountInfo: Option[BankAccountInfo] =
      Some(BankAccountInfo(
        "Jon Smith",
        SortCode("111111"),
        AccountNumber("12345678")
      ))
  }

  "the auditService" when {
    val longTimeout: Timeout = Timeout(scaled(Span(10, Seconds)))
    val hc: HeaderCarrier = HeaderCarrier()

    for ((affinityGroup, optArn) <- Seq[(AffinityGroup, Option[String])]((Individual, None), (Agent, Some("AARN1234567")), (Organisation, None)))
      s"called with an existing journeyId as ${affinityGroup.toString}" should {
        "create an audit event (generate and send audit item)" when {
          "when the ETMP call has failed" should {
            "mark etmpResult as 'Fail'" in new Setup(affinityGroup) {
              val journey: Journey = Journey(
                Some("sessionId"),
                JourneyId("1234"),
                AuditFlags(),
                JourneyTypes.RefundJourney,
                Some(Amount(Some(1234.12), None, None, totalCreditAvailableForRepayment = Some(1234.12), unallocatedCredit = Some(123.45))),
                Some(Nino("Nino")),
                None,
                Some(PaymentMethod.Card),
                None,
                None,
                None,
                None,
                None,
                testReturnUrl
              )
              auditService.auditRefundRequestEvent(journey, None, Some(affinityGroup.toString), optArn)(hc)

              eventually(longTimeout) {
                AuditStub.verifyEventAudited(
                  auditType  = "RefundRequest",
                  auditEvent = Json.parse(
                    s"""
                       |{
                       |  "etmpResult": "Fail",
                       |  "userType": "${affinityGroup.toString}",
                       |  "totalCreditAvailableForRepayment": "1234.12",
                       |  "unallocatedCredit" : "123.45",
                       |  "amountChosen": "1234.12",
                       |  "nino": "Nino",
                       |  "nrsSubmissionId" : "MissingNrsSubmissionId"
                  }""".stripMargin
                  ).as[JsObject]
                )
              }
            }
          }

          "when the ETMP call is successful" should {
            "mark etmpResult as 'Success' and include reference" in new Setup(affinityGroup) {
              val dummyRepaymentResponse = new RepaymentResponse(
                OffsetDateTime.now().minusMinutes(1),
                new RequestNumber("requestNumber")
              )

              val nrsSubmissionId: String = "e64b7df1-2cfc-46ab-ad11-0d46e2a437e5"

              val journey: Journey = Journey(
                Some("sessionId"),
                JourneyId("1234"),
                AuditFlags(),
                JourneyTypes.RefundJourney,
                Some(Amount(Some(1234.12), None, None, totalCreditAvailableForRepayment = Some(1234.12), unallocatedCredit = Some(123.45))),
                None,
                None,
                Some(PaymentMethod.Card),
                Some(AccountType("Personal")),
                testBankAccountInfo,
                None,
                None,
                Some(dummyRepaymentResponse),
                testReturnUrl
              )

              auditService.auditRefundRequestEvent(journey, Some(nrsSubmissionId), Some(affinityGroup.toString), optArn)(hc)

              eventually(longTimeout) {
                AuditStub.verifyEventAudited(
                  auditType  = "RefundRequest",
                  auditEvent = Json.parse(
                    s"""
                       |{
                       |  "etmpResult": "Success",
                       |  "userType": "${affinityGroup.toString}",
                       |  "totalCreditAvailableForRepayment": "1234.12",
                       |  "unallocatedCredit" : "123.45",
                       |  "amountChosen": "1234.12",
                       |  "reference": "requestNumber",
                       |  "nino": "MissingNino",
                       |  "nrsSubmissionId" : "$nrsSubmissionId",
                       |  "bankAccount":{
                       |      "accountType" : "Personal",
                       |      "accountHolderName" : "Jon Smith",
                       |      "sortCode" : "111111",
                       |      "accountNumber" : "12345678"
                       |    }
                       |}""".stripMargin
                  ).as[JsObject]
                )
              }
            }
          }
        }
      }
  }

  "ViewRefundStatus" when {
    "successful event" when {
      "from view and change" in {
        val expectedDetails: JsValue = Json.parse(
          s"""{
             |  "outcome": {
             |    "isSuccessful":true
             |  },
             |  "origin": "view and change",
             |  "nino": "AA111111A",
             |  "userType": "Individual",
             |  "refunds": [
             |    {
             |      "refundReference": "1",
             |      "amount": 12000,
             |      "status": "Processing",
             |      "dateRefundRequested": "2021-08-14",
             |      "repaymentMethod": "Card"
             |    },
             |    {
             |      "refundReference": "2",
             |      "amount": 76000,
             |      "status": "Approved",
             |      "dateRefundRequested": "2021-08-16",
             |      "repaymentDate": "2021-08-17",
             |      "repaymentMethod": "BACS"
             |    },
             |    {
             |      "refundReference": "3",
             |      "amount": 44000,
             |      "status": "ProcessingRisking",
             |      "dateRefundRequested": "2021-08-18",
             |      "repaymentMethod": "PaymentOrder"
             |    },
             |    {
             |      "refundReference": "4",
             |      "amount": 66000,
             |      "status": "Rejected",
             |      "dateRefundRequested": "2021-08-06",
             |      "repaymentDate": "2021-08-07"
             |    }
             |  ]
             |}""".stripMargin
        ).as[JsValue]

        val auditService = new AuditService(StubAuditConnector(expectedDetails)(testViewRefundStatusChecks))

        auditService.auditViewRefundStatus(
          taxRepayments = Some(List[TaxRepayment](
            ProcessingTaxRepayment(
              claim = Claim(
                key             = RequestNumber("1"),
                nino            = Nino("AA111111A"),
                amount          = BigDecimal(12000),
                created         = LocalDate.parse("2021-08-14", DateTimeFormatter.ISO_DATE),
                repaymentMethod = Some(PaymentMethod.Card)
              )
            ),
            ApprovedTaxRepayment(
              claim     = Claim(
                key             = RequestNumber("2"),
                nino            = Nino("AA111111A"),
                amount          = BigDecimal(76000),
                created         = LocalDate.parse("2021-08-16", DateTimeFormatter.ISO_DATE),
                repaymentMethod = Some(PaymentMethod.BACS)
              ),
              completed = LocalDate.parse("2021-08-17", DateTimeFormatter.ISO_DATE)
            ),
            ProcessingRiskingTaxRepayment(
              claim = Claim(
                key             = RequestNumber("3"),
                nino            = Nino("AA111111A"),
                amount          = BigDecimal(44000),
                created         = LocalDate.parse("2021-08-18", DateTimeFormatter.ISO_DATE),
                repaymentMethod = Some(PaymentMethod.PaymentOrder)
              )
            ),
            RejectedTaxRepayment(
              claim     = Claim(
                key             = RequestNumber("4"),
                nino            = Nino("AA111111A"),
                amount          = BigDecimal(66000),
                created         = LocalDate.parse("2021-08-06", DateTimeFormatter.ISO_DATE),
                repaymentMethod = None
              ),
              completed = LocalDate.parse("2021-08-07", DateTimeFormatter.ISO_DATE),
              message   = Some("got rejected")
            )
          )),
          affinityGroup = Some(AffinityGroup.Individual),
          maybeNino     = Some(Nino("AA111111A")),
          journeyType   = JourneyTypes.TrackJourney,
          maybeArn      = None
        )(request)
      }

      "from claim journey" in {
        val expectedDetails: JsValue = Json.parse(
          s"""{
             |  "outcome": {
             |    "isSuccessful":true
             |  },
             |  "origin": "claim journey",
             |  "nino": "AA111111A",
             |  "agentReferenceNumber":"AARN1234567",
             |  "userType": "Agent",
             |  "refunds": [
             |    {
             |      "refundReference": "2",
             |      "amount": 76000,
             |      "status": "Approved",
             |      "dateRefundRequested": "2021-08-16",
             |      "repaymentDate": "2021-08-17",
             |      "repaymentMethod": "Card"
             |    }
             |  ]
             |}""".stripMargin
        ).as[JsValue]

        val auditService = new AuditService(StubAuditConnector(expectedDetails)(testViewRefundStatusChecks))

        auditService.auditViewRefundStatus(
          taxRepayments = Some(List[TaxRepayment](
            ApprovedTaxRepayment(
              claim     = Claim(
                key             = RequestNumber("2"),
                nino            = Nino("AA111111A"),
                amount          = BigDecimal(76000),
                created         = LocalDate.parse("2021-08-16", DateTimeFormatter.ISO_DATE),
                repaymentMethod = Some(PaymentMethod.Card)
              ),
              completed = LocalDate.parse("2021-08-17", DateTimeFormatter.ISO_DATE)
            )
          )),
          affinityGroup = Some(AffinityGroup.Agent),
          maybeNino     = Some(Nino("AA111111A")),
          journeyType   = JourneyTypes.RefundJourney,
          maybeArn      = Some("AARN1234567")
        )(request)
      }
    }

    "failed event" in {
      val expectedDetails: JsValue = Json.parse(
        s"""{
             |  "outcome": {
             |    "isSuccessful":false,
             |    "failureReason":"low confidence level"
             |  },
             |  "origin": "view and change",
             |  "nino": "AA111111A",
             |  "userType": "Individual",
             |  "refunds": []
             |}""".stripMargin
      ).as[JsValue]

      val auditService = new AuditService(StubAuditConnector(expectedDetails)(testViewRefundStatusChecks))

      auditService.auditViewRefundStatus(
        taxRepayments = None,
        affinityGroup = Some(AffinityGroup.Individual),
        maybeNino     = Some(Nino("AA111111A")),
        maybeArn      = None,
        journeyType   = JourneyTypes.TrackJourney,
        failureReason = Some("low confidence level")
      )(request)
    }

  }

  "RefundAmount" when {
    "successful event" in {
      val expectedDetails: JsValue = Json.parse(
        s"""{
           |  "outcome": {
           |    "isSuccessful": true
           |  },
           |  "totalCreditAvailableForRepayment": 987.65,
           |  "unallocatedCredit": 345.67,
           |  "amountChosen": 641.98,
           |  "nino": "AA111111A",
           |  "userType": "Individual"
           |}""".stripMargin
      ).as[JsValue]

      val auditService = new AuditService(StubAuditConnector(expectedDetails)(testRefundAmount))

      auditService.auditRefundAmount(
        totalCreditAvailableForRepayment = Some(987.65),
        unallocatedCredit                = Some(345.67),
        amountChosen                     = Some(641.98),
        affinityGroup                    = Some(AffinityGroup.Individual),
        maybeNino                        = Some(Nino("AA111111A")),
        maybeArn                         = None,
        failureReason                    = None
      )(request)
    }

    "failure event" in {
      val expectedDetails: JsValue = Json.parse(
        s"""{
           |  "outcome": {
           |    "isSuccessful": false,
           |    "failureReason": "it failed"
           |  },
           |  "nino": "AA111111A",
           |  "agentReferenceNumber":"AARN1234567",
           |  "userType": "Agent"
           |}""".stripMargin
      ).as[JsValue]

      val auditService = new AuditService(StubAuditConnector(expectedDetails)(testRefundAmount))

      auditService.auditRefundAmount(
        totalCreditAvailableForRepayment = None,
        unallocatedCredit                = None,
        amountChosen                     = None,
        affinityGroup                    = Some(AffinityGroup.Agent),
        maybeNino                        = Some(Nino("AA111111A")),
        maybeArn                         = Some("AARN1234567"),
        failureReason                    = Some("it failed")
      )(request)
    }
  }

  "IdentityVerificationOutcome" when {
    for ((test, result) <- Seq(("successful", true), ("failed", false)))
      s"$test event" in {
        val expectedDetails: JsValue = Json.parse(
          s"""{
           |  "isSuccessful": ${result.toString},
           |  "nino":"AA111111A",
           |  "userType":"Individual"
           |}""".stripMargin
        ).as[JsValue]

        val auditService = new AuditService(StubAuditConnector(expectedDetails)(testIVOutcome))

        auditService.auditIVOutcome(
          isSuccessful  = result,
          affinityGroup = Some("Individual"),
          maybeNino     = Some(Nino("AA111111A"))
        )(request)
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.TripleQuestionMark"))
  case class StubAuditConnector(expectedDetails: JsValue)(callback: JsValue => ExtendedDataEvent => Any) extends AuditConnector {
    override def auditingConfig: AuditingConfig = ???
    override def auditChannel: AuditChannel = ???
    override def datastreamMetrics: DatastreamMetrics = ???

    override def sendExtendedEvent(event: ExtendedDataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] = {
      logger.info(">>>sendEvent CALLED " + event.toString)
      callback(expectedDetails)(event)
      Future.successful(AuditResult.Success)
    }
  }

  implicit val format: Format[AccountType] = Json.format[AccountType]
}
