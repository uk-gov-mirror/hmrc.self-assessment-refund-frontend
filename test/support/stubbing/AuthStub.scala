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

package support.stubbing

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.selfassessmentrefundfrontent.util.CanEqualGivens.affinityGroupCanEqual

object AuthStub {

  def authorise(
      affinityGroup:   AffinityGroup,
      confidenceLevel: ConfidenceLevel
  ): StubMapping = {

    val authoriseJsonBody = Json.obj(
      "affinityGroup" -> affinityGroup.toString,
      "confidenceLevel" -> confidenceLevel.level,
      "allEnrolments" -> allEnrolments(affinityGroup, confidenceLevel)
    )

    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.prettyPrint(authoriseJsonBody)))
    )
  }

  def allEnrolments(affinityGroup: AffinityGroup, confidenceLevel: ConfidenceLevel): JsValue =
    affinityGroup match {
      case AffinityGroup.Agent =>
        Json.arr(
          Json.obj(
            "key" -> "HMRC-MTD-IT",
            "identifiers" -> Json.arr(Json.obj(
              "key" -> "MTDITID",
              "value" -> "123"
            )),
            "state" -> "Activated",
            "confidenceLevel" -> confidenceLevel.level
          ),
          Json.obj(
            "key" -> "HMRC-AS-AGENT",
            "identifiers" -> Json.arr(Json.obj(
              "key" -> "AgentReferenceNumber",
              "value" -> "AARN1234567"
            )),
            "state" -> "Activated",
            "confidenceLevel" -> confidenceLevel.level
          )
        )
      case _ => Json.arr()
    }

  def authoriseAgentL50(): StubMapping = authorise(AffinityGroup.Agent, ConfidenceLevel.L50)
  def authoriseOrganisationL250(): StubMapping = authorise(AffinityGroup.Organisation, ConfidenceLevel.L250)
  def authoriseIndividualL250(): StubMapping = authorise(AffinityGroup.Individual, ConfidenceLevel.L250)
}
