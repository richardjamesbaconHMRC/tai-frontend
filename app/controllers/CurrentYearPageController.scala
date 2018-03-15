/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import controllers.ServiceChecks.CustomRule
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTai}
import controllers.viewModels.PotentialUnderpaymentPageVM
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.domain.PayeAccount
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.auth.ConfigProperties
import uk.gov.hmrc.tai.config.{FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.{LocalTemplateRenderer, PreferencesFrontendConnector}
import uk.gov.hmrc.tai.model.SessionData
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.{AuditConstants, TaxAccountCalculator}
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future

trait CurrentYearPageController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTai
  with Auditable
  with AuditConstants
  with TaxAccountCalculator
  with FeatureTogglesConfig {

  def taiService: TaiService
  def codingComponentService: CodingComponentService
  def auditService: AuditService

  def preferencesFrontendConnector: PreferencesFrontendConnector

  def activatePaperless: Boolean

  def activatePaperlessEvenIfGatekeeperFails: Boolean

  def activityLoggerService: ActivityLoggerService

  def reliefsPage(): Action[AnyContent] = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user => implicit sessionData => implicit request =>
      getReliefsPage(Nino(user.getNino))
  }

  def getReliefsPage(nino: Nino)(implicit request: Request[AnyContent], user: TaiUser, sessionData: SessionData): Future[Result] = {

    sendActingAttorneyAuditEvent("getReliefsPage")
    val rule: CustomRule = details => Future.successful(Ok(views.html.reliefs(details)))

    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }
  } recoverWith handleErrorResponse("getReliefsPage", nino)

}

object CurrentYearPageController extends CurrentYearPageController with AuthenticationConnectors {

  override val taiService = TaiService
  override val codingComponentService: CodingComponentService = CodingComponentService
  override val auditService = AuditService
  override val activityLoggerService = ActivityLoggerService

  val preferencesFrontendConnector = PreferencesFrontendConnector
  lazy val activatePaperless: Boolean = ConfigProperties.activatePaperless
  lazy val activatePaperlessEvenIfGatekeeperFails: Boolean = ConfigProperties.activatePaperlessEvenIfGatekeeperFails

  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: PartialRetriever = TaiHtmlPartialRetriever

}

