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

package uk.gov.hmrc.selfassessmentrefundfrontend.testonly.connectors

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsObject
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReadsInstances.readUnit
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.selfassessmentrefundfrontend.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
final case class StubsAdminConnector @Inject() (client: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext) {

  def createAccount(nino: String, ifNotExists: Boolean = false)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = s"${config.selfAssessmentRefundStubsUrl}/admin/accounts/$nino"

    if (ifNotExists) {
      client.post(url"$url")
        .withBody(JsObject.empty)
        .execute[Unit]
    } else {
      client.put(url"$url")
        .withBody(JsObject.empty)
        .execute[Unit]
    }
  }

  def updateAccount(nino: String, mock: Boolean = false)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = s"${config.selfAssessmentRefundStubsUrl}/admin/accounts/$nino?mock=${mock.toString}"

    client.put(url"$url")
      .withBody(JsObject.empty)
      .execute[Unit]
  }

}

object StubsAdminConnector {

  final case class CreateTaxAccount()

}
