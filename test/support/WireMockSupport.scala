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

package support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.{CookieHeaderEncoding, Session, SessionCookieBaker}
import uk.gov.hmrc.crypto.{Encrypter, PlainText}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfassessmentrefundfrontend.audit.model.AuditFlags
import uk.gov.hmrc.selfassessmentrefundfrontend.connectors.barsLockout.model.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import uk.gov.hmrc.selfassessmentrefundfrontend.model._
import uk.gov.hmrc.selfassessmentrefundfrontend.model.customer.Nino
import uk.gov.hmrc.selfassessmentrefundfrontend.model.journey.{Journey, JourneyType, JourneyTypes}
import uk.gov.hmrc.selfassessmentrefundfrontend.model.repayment.{RepaymentCreatedResponse, RequestNumber}
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdAll._
import uk.gov.hmrc.selfassessmentrefundfrontend.testdata.TdBars

import java.net.{URI, URL}
import java.time.Instant.now
import java.time.{Instant, OffsetDateTime}

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>

  implicit val wireMockServer: WireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

  wireMockServer.start()

  WireMock.configureFor("localhost", wireMockServer.port())

  val wireMockHost = "localhost"

  lazy val wireMockBaseUrlAsString: String = s"http://$wireMockHost:${wireMockServer.port.toString}"
  lazy val wireMockBaseUrl: URL = new URI(wireMockBaseUrlAsString).toURL

  override def beforeEach(): Unit = WireMock.reset()

  override protected def afterAll(): Unit = wireMockServer.stop()

  val testAmount: Amount = Amount(Some(BigDecimal(123)), None, None, availableCredit = Some(BigDecimal(123)), balanceDueWithin30Days = Some(BigDecimal(45)))
  val testRepaymentResponse: RepaymentResponse = RepaymentResponse(OffsetDateTime.parse("2023-12-01T17:35:30+01:00"), RequestNumber("1234567890"))

  def stubBackendJourneyId(): StubMapping =
    stubFor(get(urlEqualTo("/self-assessment-refund-backend/journey/find-latest-by-session-id"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Journey(Some(sessionId), journeyId, AuditFlags(), JourneyTypes.RefundJourney, None, None, None, None, None, None, None, None, None, None)).toString)))

  def stubBackendJourneyNoSessionId(): StubMapping =
    stubFor(get(urlEqualTo("/self-assessment-refund-backend/journey/find-latest-by-session-id"))
      .willReturn(aResponse()
        .withStatus(BAD_REQUEST)
        .withBody(Json.parse("""{"statusCode": 400,"message": "Request does not have sessionId"}""").toString)))

  def stubBackendJourney(): StubMapping = stubFor(post(urlEqualTo("/self-assessment-refund-backend/journeyid"))
    .willReturn(aResponse()
      .withStatus(OK)
      .withBody(Json.toJson(journeyId).toString)))

  def stubBackendLastPaymentMethod(method: PaymentMethod): StubMapping =
    stubFor(get(urlPathMatching("/self-assessment-refund-backend/last-payment/.*"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(method).toString)))

  def stubBackendBusinessJourney(
      nino:              Option[Nino]              = None,
      method:            Option[PaymentMethod]     = None,
      backReturnUrl:     Boolean                   = false,
      amount:            Option[Amount]            = Some(testAmount),
      hasStartedReauth:  Option[Boolean]           = Some(true),
      repaymentResponse: Option[RepaymentResponse] = Some(testRepaymentResponse),
      journeyType:       JourneyType               = JourneyTypes.RefundJourney
  ): StubMapping =
    stubFor(get(urlEqualTo("/self-assessment-refund-backend/journey/find-latest-by-session-id"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Journey(
          Some(sessionId),
          journeyId,
          AuditFlags(),
          journeyType,
          amount,
          nino,
          None,
          method,
          Some(AccountType("Business")),
          Some(bankAccountInfo),
          None,
          hasStartedReauth,
          repaymentResponse,
          if (backReturnUrl) Some(ReturnUrl("/returnUrl")) else None
        )).toString)))

  def stubBackendPersonalJourney(nino: Option[Nino] = None, method: Option[PaymentMethod] = None, amount: Amount = testAmount): StubMapping =
    stubFor(get(urlEqualTo("/self-assessment-refund-backend/journey/find-latest-by-session-id"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Journey(
          Some(sessionId),
          journeyId,
          AuditFlags(),
          JourneyTypes.RefundJourney,
          Some(amount),
          nino,
          None,
          method,
          Some(AccountType("Personal")),
          Some(BankAccountInfo(
            "name",
            SortCode("111111"),
            AccountNumber("12345678")
          )),
          None,
          None,
          Some(RepaymentResponse(OffsetDateTime.parse("2023-12-01T17:35:30+01:00"), RequestNumber("1234567890"))),
          None
        )).toString)))

  def stubBarsVerifyStatus(lockedOut: Boolean = false): StubMapping = {
    val barsVerifyStatusResponse = {
      if (lockedOut)
        BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts.zero, Some(TdBars.futureDateTime))
      else
        BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts.zero, None)
    }

    stubFor(post(urlEqualTo("/self-assessment-refund-backend/bars/verify/status"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(barsVerifyStatusResponse).toString)))
  }

  /**
   * Use to get through an authenticated journey action without a lock out, when you want a lockout after the new verify check
   */
  def stubTwoBarsVerifyStatusFailedSecondWithLockout(): StubMapping = {
    stubFor(post(urlEqualTo("/self-assessment-refund-backend/bars/verify/status"))
      .inScenario("Not locked out at authenticated journey action, locking out after next verify check")
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(2), None)).toString))
      .willSetStateTo("Next failed verify check locks out"))

    stubFor(post(urlEqualTo("/self-assessment-refund-backend/bars/verify/status"))
      .inScenario("Not locked out at authenticated journey action, locking out after next verify check")
      .whenScenarioStateIs("Next failed verify check locks out")
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(
          BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(3), Some(Instant.now().minusSeconds(86400)))
        ).toString)))
  }

  def stubBarsVerifyUpdateWithLockout(): StubMapping =
    stubFor(post(urlEqualTo(s"/self-assessment-refund-backend/bars/verify/update"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(
          BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(3), Some(Instant.now().minusSeconds(86400)))
        ).toString)))

  def stubLatestBySessionId(nino: Option[Nino] = None): StubMapping =
    stubFor(get(urlEqualTo("/self-assessment-refund-backend/journey/find-latest-by-session-id"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Journey(Some(sessionId), journeyId, AuditFlags(), JourneyTypes.RefundJourney, None, nino, None, None, Some(AccountType("Personal")), None, None, None, None, None)).toString)))

  def stubPOSTJourney(): StubMapping = {
    stubFor(post(urlPathMatching("/self-assessment-refund-backend/journey/.*"))
      .willReturn(aResponse()
        .withStatus(OK)))
  }

  def stubPOSTJourneyError(): StubMapping = {
    stubFor(post(urlPathMatching("/self-assessment-refund-backend/journey/.*"))
      .willReturn(aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)))
  }

  def stubGETJourneyWithBankDetails(name: String = "", amount: Amount = testAmount): StubMapping =
    stubFor(get(urlEqualTo("/self-assessment-refund-backend/journey/find-latest-by-session-id"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Journey(Some(sessionId), journeyId, AuditFlags(), JourneyTypes.RefundJourney, Some(amount), Some(nino), None, None, Some(AccountType("Business")), Some(BankAccountInfo(name, SortCode("111111"), AccountNumber("12345678"))), None, None, None, None)).toString)))

  def stubGETBackendAudit(): StubMapping =
    stubFor(get(urlPathMatching("/self-assessment-refund-backend/audit/.*"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Amount(Some(123), None, None, Some(123), Some(123))).toString)))

  def stubPOSTBackendAudit(): StubMapping =
    stubFor(post(urlPathMatching("/self-assessment-refund-backend/audit/.*"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Amount(Some(123), None, None, Some(123), Some(123))).toString)))

  def stubCreateRepayment(createRepaymentRequest: CreateRepaymentRequest, submissionId: String = "submissionId"): StubMapping =
    stubFor(post(urlPathMatching("/self-assessment-refund-backend/repayments/.*"))
      .withRequestBody(equalToJson(Json.toJson(createRepaymentRequest).toString()))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(RepaymentCreatedResponse(no1, Some(submissionId))).toString)))

  def stubCreateRepaymentNoNRSSubmissionId(createRepaymentRequest: CreateRepaymentRequest): StubMapping =
    stubFor(post(urlPathMatching("/self-assessment-refund-backend/repayments/.*"))
      .withRequestBody(equalToJson(Json.toJson(createRepaymentRequest).toString()))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(RepaymentCreatedResponse(no1, None)).toString)))

  def stubCreateRepaymentError(createRepaymentRequest: CreateRepaymentRequest): StubMapping =
    stubFor(
      post(urlPathMatching("/self-assessment-refund-backend/repayments/.*"))
        .withRequestBody(equalToJson(Json.toJson(createRepaymentRequest).toString()))
        .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
    )

  def stubRepaymentWithNumber(): StubMapping =
    stubFor(get(urlPathMatching("/self-assessment-refund-backend/repayments/.*"))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Some(RepaymentResponse(OffsetDateTime.now(), RequestNumber("1234567890")))).toString)))

  def stubGetRepaymentError(): StubMapping = {
    stubFor(get(urlPathMatching("/self-assessment-refund-backend/repayments/.*"))
      .willReturn(aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)))
  }

  def verifyUpdateJourneyCalled(journey: Journey) =
    verify(postRequestedFor(
      urlEqualTo(s"/self-assessment-refund-backend/journey/${journey.id.value}")
    )
      .withRequestBody(equalToJson(Json.toJson(journey).toString())))

  // FROM: https://github.com/hmrc/bank-account-coc-frontend/blob/9fabd5ee7b08fca059f77f56286cbf1fd0522b34/it/testsupport/AuthResponses.scala
  def givenASuccessfulLogInResponse(crypto: Encrypter, baker: SessionCookieBaker, encode: CookieHeaderEncoding): Unit = {
    //Implementation based on CookieCryptoFilter trait and auth-login-stub project

    val sessionId = "12345678"

    val session = Session(Map(
      SessionKeys.sessionId -> sessionId,
      SessionKeys.lastRequestTimestamp -> now().toEpochMilli.toString
    ))

    val rawCookie = baker.encodeAsCookie(session)
    val crypted = crypto.encrypt(PlainText(rawCookie.value))
    val cookie = rawCookie.copy(value = crypted.value)
    val headerValue = encode.encodeSetCookieHeader(List(cookie))

    stubFor(
      get(urlPathEqualTo("/auth-login-stub/gg-sign-in"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("You have been logged in")
            .withHeader(HeaderNames.SET_COOKIE, headerValue)
        )
    )
    ()
  }

  def givenTheUserIsAuthorised(): Unit = {
    val vrn = "220432715"

    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(
              s"""{
                 "affinityGroup": "Individual",
                 "nino": "AA111111A",
                 "allEnrolments": [
                   {
                     "key": "HMRC-NI",
                     "identifiers": [
                       {
                         "key": "VRN",
                         "value": "$vrn"
                       }
                     ],
                     "state": "Activated"
                   }
                 ],
                 "credentials": {
                   "providerId": "12345-credId",
                   "providerType": "GovernmentGateway"
                 }
               }"""
            )
        )
    )
    ()
  }

  def givenTheUserIsAuthorisedAsAgent(): Unit = {
    val arn = "1234567"

    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(
              s"""{
                 "affinityGroup": "Agent",
                 "nino": "AA111111A",
                 "confidenceLevel": 250,
                 "allEnrolments": [
                   {
                     "key": "HMRC-AS-AGENT",
                     "identifiers": [
                       {
                         "key": "AgentReferenceNumber",
                         "value": "$arn"
                       }
                     ],
                     "state": "Activated"
                   }
                 ],
                  "delegatedAuthRule":"mtd-it-auth",
                  "state":"Activated",
                  "enrolment":"HMRC-MTD-IT",
                  "identifiers":[
                     {
                      "key":"MTDITID",
                      "value":"123"
                      }
                   ],
                 "identifiers":[
                    {
                      "key":"NINO",
                      "value":"AB111111C"
                    }
                  ],
                    "state":"Activated",
                    "enrolment":""
                  ,
                 "credentials": {
                   "providerId": "12345-credId",
                   "providerType": "GovernmentGateway"
                 }
               }"""
            )
        )
    )
    ()
  }
}
