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

package uk.gov.hmrc.selfassessmentrefundfrontend.views.components

import play.api.data.Form
import play.api.i18n.Messages

object PageTitle {
  def apply(h1: String)(implicit messages: Messages): String = makeTitle(h1, "", None)
  def apply(h1: String, serviceName:       String)(implicit messages: Messages): String = makeTitle(h1, serviceName, None)
  def apply(h1: String, serviceName: String, form: Form[_])(implicit messages: Messages): String =
    makeTitle(h1, serviceName, Some(form))
  def errorPageTitle(h1: String)(implicit messages: Messages): String = s"""$h1 - ${Messages("service.title.suffix")}"""

  private def makeTitle(
    h1:          String,
    serviceName: String,
    maybeForm:   Option[Form[_]]
  )(implicit messages: Messages): String = {
    // TODO: Line below will have no service name part if was not supplied. This to be checked in OPS-12750 (or other ticket) that each page has the correct title.
    val title: String = s"""$h1 -${if (serviceName.nonEmpty) s" $serviceName -" else ""} ${Messages(
        "service.title.suffix"
      )}"""
    if (maybeForm.exists(_.hasErrors)) s"""${Messages("service.title.error-prefix")} $title""" else title
  }
}
