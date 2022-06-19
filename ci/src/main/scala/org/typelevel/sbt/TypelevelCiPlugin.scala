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

import com.typesafe.tools.mima.plugin.MimaPlugin
import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._
import org.typelevel.sbt.gha.GitHubActionsPlugin
import sbt._

import scala.language.experimental.macros

object TypelevelCiPlugin extends AutoPlugin {

  override def requires = GitHubActionsPlugin && GenerativePlugin && MimaPlugin
  override def trigger = allRequirements

  object autoImport {
    def tlCrossRootProject: CrossRootProject = macro CrossRootProjectMacros.crossRootProjectImpl

    lazy val tlCiHeaderCheck =
      settingKey[Boolean]("Whether to do header check in CI (default: false)")
    lazy val tlCiScalafmtCheck =
      settingKey[Boolean]("Whether to do scalafmt check in CI (default: false)")
    lazy val tlCiScalafixCheck =
      settingKey[Boolean]("Whether to do scalafix check in CI (default: false)")
    lazy val tlCiMimaBinaryIssueCheck =
      settingKey[Boolean]("Whether to do MiMa binary issues check in CI (default: true)")
    lazy val tlCiDocCheck =
      settingKey[Boolean]("Whether to build API docs in CI (default: true)")
  }

  import autoImport._

  override def buildSettings = Seq(
    tlCiHeaderCheck := false,
    tlCiScalafmtCheck := false,
    tlCiScalafixCheck := false,
    tlCiMimaBinaryIssueCheck := true,
    tlCiDocCheck := true,
    githubWorkflowTargetBranches ++= Seq(
      "!update/**", // ignore steward branches
      "!pr/**" // escape-hatch to disable ci on a branch
    ),
    githubWorkflowPublishTargetBranches := Seq(),
    githubWorkflowBuild := {

      val style = (tlCiHeaderCheck.value, tlCiScalafmtCheck.value) match {
        case (true, true) => // headers + formatting
          List(
            WorkflowStep.Sbt(
              List("headerCheckAll", "scalafmtCheckAll", "project /", "scalafmtSbtCheck"),
              name = Some("Check headers and formatting"),
              cond = Some(primaryJavaCond.value)
            )
          )
        case (true, false) => // headers
          List(
            WorkflowStep.Sbt(
              List("headerCheckAll"),
              name = Some("Check headers"),
              cond = Some(primaryJavaCond.value)
            )
          )
        case (false, true) => // formatting
          List(
            WorkflowStep.Sbt(
              List("scalafmtCheckAll", "project /", "scalafmtSbtCheck"),
              name = Some("Check formatting"),
              cond = Some(primaryJavaCond.value)
            )
          )
        case (false, false) => Nil // nada
      }

      val test = List(
        WorkflowStep.Sbt(List("test"), name = Some("Test"))
      )

      val scalafix =
        if (tlCiScalafixCheck.value)
          List(
            WorkflowStep.Sbt(
              List("scalafixAll --check"),
              name = Some("Check scalafix lints"),
              cond = Some(primaryJavaCond.value)
            )
          )
        else Nil

      val mima =
        if (tlCiMimaBinaryIssueCheck.value)
          List(
            WorkflowStep.Sbt(
              List("mimaReportBinaryIssues"),
              name = Some("Check binary compatibility"),
              cond = Some(primaryJavaCond.value)
            ))
        else Nil

      val doc =
        if (tlCiDocCheck.value)
          List(
            WorkflowStep.Sbt(
              List("doc"),
              name = Some("Generate API documentation"),
              cond = Some(primaryJavaCond.value)
            )
          )
        else Nil

      style ++ test ++ scalafix ++ mima ++ doc
    },
    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"))
  )

  private val primaryJavaCond = Def.setting {
    val java = githubWorkflowJavaVersions.value.head
    s"matrix.java == '${java.render}'"
  }

}
