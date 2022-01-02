/*
 * Copyright 2022 Typelevel
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

package org.typelevel.sbt

import sbt._, Keys._
import sbtghactions.GenerativePlugin
import sbtghactions.GitHubActionsPlugin

import de.heikoseeberger.sbtheader.{AutomateHeaderPlugin, HeaderPlugin, License, SpdxLicense}

object TypelevelPlugin extends AutoPlugin {

  override def requires =
    TypelevelKernelPlugin &&
      TypelevelSettingsPlugin &&
      TypelevelVersioningPlugin &&
      TypelevelMimaPlugin &&
      TypelevelCiPlugin &&
      GitHubActionsPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val tlFatalWarningsInCi = settingKey[Boolean](
      "Convert compiler warnings into errors under CI builds (default: true)")
  }

  import autoImport._
  import TypelevelKernelPlugin.autoImport._
  import TypelevelSettingsPlugin.autoImport._
  import GenerativePlugin.autoImport._
  import GitHubActionsPlugin.autoImport._
  import HeaderPlugin.autoImport._

  override def globalSettings = Seq(
    tlFatalWarningsInCi := true
  )

  override def buildSettings = Seq(
    organization := "org.typelevel",
    organizationName := "Typelevel",
    startYear := Some(java.time.YearMonth.now().getYear()),
    licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/"),
    Def.derive(tlFatalWarnings := (tlFatalWarningsInCi.value && githubIsWorkflowBuild.value)),
    githubWorkflowBuildMatrixExclusions ++= {
      for {
        // default scala is last in the list, default java first
        scala <- githubWorkflowScalaVersions.value.init
        java <- githubWorkflowJavaVersions.value.tail
      } yield MatrixExclude(Map("scala" -> scala, "java" -> java.render))
    }
  ) ++ replaceCommandAlias(
    "ci",
    "; project /; headerCheckAll; scalafmtCheckAll; scalafmtSbtCheck; clean; test; mimaReportBinaryIssues"
  )

  override def projectSettings = AutomateHeaderPlugin.projectSettings

}
