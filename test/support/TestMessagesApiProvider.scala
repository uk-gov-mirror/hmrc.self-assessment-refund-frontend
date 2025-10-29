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

import play.api.http.HttpConfiguration
import play.api.i18n._
import play.api.{Configuration, Environment, Logging}

import javax.inject.Inject

class TestMessagesApiProvider @Inject() (
  environment:       Environment,
  config:            Configuration,
  langs:             Langs,
  httpConfiguration: HttpConfiguration
) extends DefaultMessagesApiProvider(environment, config, langs, httpConfiguration)
    with Logging {

  override lazy val get: MessagesApi =
    new DefaultMessagesApi(
      loadAllMessages,
      langs,
      langCookieName,
      langCookieSecure,
      langCookieHttpOnly,
      langCookieSameSite,
      httpConfiguration,
      langCookieMaxAge
    ) {
      override protected def noMatch(key: String, args: Seq[Any])(implicit lang: Lang): String = {
        logger.error(s"Could not find message for key: $key ${args.mkString("-")}")
        s"""not_found_message("$key")"""
      }
    }

}
