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

import org.typelevel.sbt.NoPublishGlobalPlugin.noPublishModulesIgnore
import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._
import org.typelevel.sbt.gha.GitHubActionsPlugin
import org.typelevel.sbt.gha.WorkflowStep
import sbt._

import scala.language.experimental.macros

object TypelevelCiPlugin extends AutoPlugin {

  override def requires = GitHubActionsPlugin && GenerativePlugin
  override def trigger = allRequirements

  object autoImport {
    def tlCrossRootProject: CrossRootProject = macro CrossRootProjectMacros.crossRootProjectImpl

    lazy val tlCiHeaderCheck =
      settingKey[Boolean]("Whether to do header check in CI (default: false)")
    lazy val tlCiScalafmtCheck =
      settingKey[Boolean]("Whether to do scalafmt check in CI (default: false)")
    lazy val tlCiJavafmtCheck =
      settingKey[Boolean]("Whether to do javafmt check in CI (default: false)")
    lazy val tlCiScalafixCheck =
      settingKey[Boolean]("Whether to do scalafix check in CI (default: false)")
    lazy val tlCiMimaBinaryIssueCheck =
      settingKey[Boolean]("Whether to do MiMa binary issues check in CI (default: false)")
    lazy val tlCiDocCheck =
      settingKey[Boolean]("Whether to build API docs in CI (default: false)")

    lazy val tlCiDependencyGraphJob =
      settingKey[Boolean]("Whether to add a job to submit dependencies to GH (default: true)")

    lazy val tlCiStewardValidateConfig = settingKey[Option[File]](
      "The location of the Scala Steward config to validate (default: `.scala-steward.conf`, if exists)")

    lazy val tlCiForkCondition =
      settingKey[String](
        "Condition for checking on CI whether this project is a fork of another (default: `github.event.repository.fork == false`)")

  }

  import autoImport._

  override def buildSettings = Seq(
    tlCiHeaderCheck := false,
    tlCiScalafmtCheck := false,
    tlCiJavafmtCheck := false,
    tlCiScalafixCheck := false,
    tlCiMimaBinaryIssueCheck := false,
    tlCiDocCheck := false,
    tlCiDependencyGraphJob := true,
    tlCiForkCondition := "github.event.repository.fork == false",
    githubWorkflowTargetBranches ++= Seq(
      "!update/**", // ignore steward branches
      "!pr/**" // escape-hatch to disable ci on a branch
    ),
    githubWorkflowPublishTargetBranches := Seq(),
    githubWorkflowBuild := {

      val style = {
        val headers = List("headerCheckAll").filter(_ => tlCiHeaderCheck.value)

        val scalafmt = List(
          "scalafmtCheckAll",
          "project /",
          "scalafmtSbtCheck"
        ).filter(_ => tlCiScalafmtCheck.value)

        val javafmt = List("javafmtCheckAll").filter(_ => tlCiJavafmtCheck.value)

        val formatting = javafmt ++ scalafmt

        val headersFormatting = headers ++ formatting

        val names =
          List("headers").filter(_ => headers.nonEmpty) ++ List("formatting").filter(_ =>
            formatting.nonEmpty)

        List(
          WorkflowStep.Sbt(
            headers ++ formatting,
            name = Some(s"Check ${names.mkString(" and ")}"),
            cond = Some(primaryAxisCond.value)
          )
        ).filter(_ => headersFormatting.nonEmpty)
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
              cond = Some(primaryAxisCond.value)
            )
          )
        else Nil

      val mima =
        if (tlCiMimaBinaryIssueCheck.value)
          List(
            WorkflowStep.Sbt(
              List("mimaReportBinaryIssues"),
              name = Some("Check binary compatibility"),
              cond = Some(primaryAxisCond.value)
            ))
        else Nil

      val doc =
        if (tlCiDocCheck.value)
          List(
            WorkflowStep.Sbt(
              List("doc"),
              name = Some("Generate API documentation"),
              cond = Some(primaryAxisCond.value)
            )
          )
        else Nil

      style ++ scalafix ++ test ++ mima ++ doc
    },
    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8")),
    githubWorkflowAddedJobs ++= {
      val ghEventCond = "github.event_name != 'pull_request'"
      val jobCond = s"${tlCiForkCondition.value} && $ghEventCond"

      val dependencySubmission =
        if (tlCiDependencyGraphJob.value)
          List(
            WorkflowJob(
              "dependency-submission",
              "Submit Dependencies",
              scalas = Nil,
              sbtStepPreamble = Nil,
              javas = List(githubWorkflowJavaVersions.value.head),
              steps = githubWorkflowJobSetup.value.toList :+
                WorkflowStep.DependencySubmission(
                  None,
                  Some(noPublishModulesIgnore.value.toList),
                  Some(List("test", "scala-tool", "scala-doc-tool", "test-internal")),
                  None
                ),
              cond = Some(jobCond)
            ))
        else Nil

      dependencySubmission
    },
    tlCiStewardValidateConfig :=
      Some(file(".scala-steward.conf")).filter(_.exists()),
    githubWorkflowAddedJobs ++= {
      tlCiStewardValidateConfig.value.toList.map { config =>
        WorkflowJob(
          "validate-steward",
          "Validate Steward Config",
          WorkflowStep.Checkout ::
            WorkflowStep.SetupJava(List(JavaSpec.temurin("11")), false) :::
            WorkflowStep.Use(
              UseRef.Public("coursier", "setup-action", "v1"),
              Map("apps" -> "scala-steward")
            ) ::
            WorkflowStep.Run(List(s"scala-steward validate-repo-config $config")) :: Nil,
          scalas = List.empty
        )
      }
    }
  )

  override def projectSettings: Seq[Setting[_]] = Seq(
    Test / Keys.executeTests := {
      val results: Tests.Output = (Test / Keys.executeTests).value
      GitHubActionsPlugin.appendToStepSummary(
        renderTestResults(Keys.thisProject.value.id, Keys.scalaVersion.value, results)
      )
      results
    }
  )

  private def renderTestResults(
      projectName: String,
      scalaVersion: String,
      results: Tests.Output): String = {

    val testHeader: String =
      s"""|### ${projectName} Tests Results: ${results.overall}
          |To run them locally use `++${scalaVersion} ${projectName}/test`
          |""".stripMargin

    val tableHeader: String =
      s"""|<details>
          |
          ||SuiteName|Result|Passed|Failed|Errors|Skipped|Ignored|Canceled|Pending|
          ||-:|-|-|-|-|-|-|-|-|
          |""".stripMargin

    val tableBody = results.events.map {
      case (suiteName, suiteResult) =>
        List(
          suiteName,
          suiteResult.result.toString(),
          suiteResult.passedCount.toString(),
          suiteResult.failureCount.toString(),
          suiteResult.errorCount.toString(),
          suiteResult.skippedCount.toString(),
          suiteResult.ignoredCount.toString(),
          suiteResult.canceledCount.toString(),
          suiteResult.pendingCount.toString()
        ).mkString("|", "|", "|")
    }

    val table: String = tableBody.mkString(tableHeader, "\n", "\n</details>\n\n")

    if (results.events.nonEmpty)
      testHeader + table
    else ""
  }

  private val primaryAxisCond = Def.setting {
    val java = githubWorkflowJavaVersions.value.head
    val os = githubWorkflowOSes.value.head

    // disjoint keys have unique sources so this condition should not consider them
    val disjointKeys = githubWorkflowArtifactDownloadExtraKeys.value
    val additionalAxes = githubWorkflowBuildMatrixAdditions
      .value
      .toList
      .collect {
        case (k, primary :: _) if !disjointKeys.contains(k) =>
          s" && matrix.$k == '$primary'"
      }
      .mkString

    s"matrix.java == '${java.render}' && matrix.os == '${os}'$additionalAxes"
  }

}
