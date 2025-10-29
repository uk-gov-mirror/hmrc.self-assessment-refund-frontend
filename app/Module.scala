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

import com.google.inject.{AbstractModule, Provides, Singleton}
import play.api.Logger
import play.api.http.HttpConfiguration
import play.api.i18n._

import java.time.Clock

class Module extends AbstractModule {
  private lazy val logger = Logger(getClass)

  override def configure(): Unit = ()

  @Provides
  @Singleton
  def clockProvider: Clock = Clock.systemUTC()

  @Provides
  @Singleton
  def i18nSupport(
    m:                 MessagesApi,
    langs:             Langs,
    httpConfiguration: HttpConfiguration
  ): I18nSupport = new I18nSupport {
    override def messagesApi: MessagesApi = new DefaultMessagesApi(
      messages = m.messages,
      langs = langs,
      langCookieName = m.langCookieName,
      langCookieSecure = m.langCookieSecure,
      langCookieHttpOnly = m.langCookieHttpOnly,
      httpConfiguration = httpConfiguration
    ) {

      // return the key wrapped around a specific pattern so that automated tests can detect
      // stray message keys more easily
      override protected def noMatch(key: String, args: Seq[Any])(implicit lang: Lang): String = {
        logger.error(s"Could not find message for key: $key")
        s"""message("$key")"""
      }
    }
  }

}
