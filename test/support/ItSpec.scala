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

import org.apache.pekko.stream.Materializer
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

abstract class ItSpec
    extends AnyWordSpec
    with WireMockSupport
    with OptionValues
    with GuiceOneAppPerSuite
    with Matchers {

  def fakeAuthConnector: Option[AuthConnector] = Some(
    new AuthConnector {
      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[A] = {
        val jsonBody = Json.obj(
          "affinityGroup"   -> "Individual",
          "confidenceLevel" -> 250,
          "allEnrolments"   -> Json.arr()
        )
        Future.successful(Json.toJson(jsonBody).as[A](retrieval.reads))
      }
    }
  )

  protected lazy val configOverrides: Map[String, Any] = Map()

  protected lazy val configMap: Map[String, Any] = Map[String, Any](
    "metrics.jvm"                                               -> false,
    "metrics.enabled"                                           -> false,
    "microservice.services.self-assessment-refund-backend.port" -> wireMockServer.port(),
    "microservice.services.auth.port"                           -> wireMockServer.port(),
    "microservice.services.bank-account-reputation.port"        -> wireMockServer.port()
  ) ++ configOverrides

  protected lazy val configMapWithAuditing: Map[String, Any] = Map[String, Any](
    "auditing.enabled"               -> true,
    "auditing.consumer.baseUri.port" -> wireMockServer.port
  ) ++ configMap

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(fakeAuthConnector.toList.map(connector => bind[AuthConnector].toInstance(connector)))
      .configure(configMap)
      .overrides(bind[MessagesApi].toProvider[TestMessagesApiProvider])
      .build()

  def fakeApplicationWithAuditing(): Application =
    new GuiceApplicationBuilder()
      .overrides(fakeAuthConnector.toList.map(connector => bind[AuthConnector].toInstance(connector)))
      .configure(configMapWithAuditing)
      .overrides(bind[MessagesApi].toProvider[TestMessagesApiProvider])
      .build()

  implicit val mat: Materializer = fakeApplication().injector.instanceOf[Materializer]
}
