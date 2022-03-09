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
import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GitHubActionsPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin

import scala.collection.immutable

object TypelevelPlugin extends AutoPlugin {

  override def requires =
    TypelevelKernelPlugin &&
      TypelevelSettingsPlugin &&
      TypelevelCiReleasePlugin &&
      GitHubActionsPlugin &&
      HeaderPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val tlFatalWarningsInCi = settingKey[Boolean](
      "Convert compiler warnings into errors under CI builds (default: true)")
  }

  import autoImport._
  import TypelevelKernelPlugin.mkCommand
  import TypelevelSettingsPlugin.autoImport._
  import TypelevelSonatypeCiReleasePlugin.autoImport._
  import GenerativePlugin.autoImport._
  import GitHubActionsPlugin.autoImport._

  override def globalSettings = Seq(
    tlFatalWarningsInCi := true
  )

  override def buildSettings = Seq(
    organization := "org.typelevel",
    organizationName := "Typelevel",
    organizationHomepage := {
      organizationHomepage.?.value.flatten.orElse {
        if (organizationName.value == "Typelevel")
          Some(url("https://typelevel.org"))
        else None
      }
    },
    startYear := Some(java.time.YearMonth.now().getYear()),
    licenses += "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"),
    tlCiReleaseBranches := Seq("main"),
    Def.derive(tlFatalWarnings := (tlFatalWarningsInCi.value && githubIsWorkflowBuild.value)),
    githubWorkflowBuildMatrixExclusions ++= {
      val defaultScala = (ThisBuild / scalaVersion).value
      for {
        scala <- githubWorkflowScalaVersions.value.filterNot(_ == defaultScala)
        java <- githubWorkflowJavaVersions.value.tail // default java is head
      } yield MatrixExclude(Map("scala" -> scala, "java" -> java.render))
    },
    githubWorkflowBuild := {
      WorkflowStep.Sbt(
        List("headerCheckAll", "scalafmtCheckAll", "project /", "scalafmtSbtCheck"),
        name = Some("Check headers and formatting"),
        cond = Some(primaryJavaCond.value)
      ) +: githubWorkflowBuild.value
    }
  ) ++ addCommandAlias(
    "prePR",
    mkCommand(
      List(
        "reload",
        "project /",
        "clean",
        "githubWorkflowGenerate",
        "headerCreateAll",
        "scalafmtAll",
        "scalafmtSbt",
        "set ThisBuild / tlFatalWarnings := tlFatalWarningsInCi.value",
        "Test / compile",
        "mimaReportBinaryIssues",
        "reload"
      )
    )
  ) ++ addCommandAlias(
    "tlPrePrBotHook",
    mkCommand(
      List(
        "githubWorkflowGenerate",
        "+headerCreateAll",
        "+scalafmtAll",
        "scalafmtSbt"
      )
    )
  )

  // override for bincompat
  override def projectSettings = immutable.Seq.empty

  private val primaryJavaCond = Def.setting {
    val java = githubWorkflowJavaVersions.value.head
    s"matrix.java == '${java.render}'"
  }

}
