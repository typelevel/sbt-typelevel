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

import sbt._
import sbtghactions.GenerativePlugin
import sbtghactions.GitHubActionsPlugin
import sbtghactions.GenerativePlugin.autoImport._
import com.typesafe.tools.mima.plugin.MimaPlugin
import TypelevelKernelPlugin.mkCommand

object TypelevelCiPlugin extends AutoPlugin {

  override def requires = GitHubActionsPlugin && GenerativePlugin && MimaPlugin
  override def trigger = allRequirements

  object autoImport {
    def tlCrossRootProject: CrossRootProject = CrossRootProject()
  }

  override def buildSettings =
    addCommandAlias("ci", mkCommand(ciCommands)) ++ Seq(
      githubWorkflowPublishTargetBranches := Seq(),
      githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("ci"))),
      githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8")),
      githubWorkflowGeneratedUploadSteps ~= { steps =>
        // hack hack hack until upstreamed
        // workaround for https://github.com/djspiewak/sbt-github-actions/pull/66
        steps.headOption match {
          case Some(
                WorkflowStep
                  .Run(command :: _, _, Some("Compress target directories"), _, _, _)) =>
            val mkdirStep = WorkflowStep.Run(
              commands = List(command.replace("tar cf targets.tar", "mkdir -p")),
              name = Some("Make target directories")
            )
            mkdirStep +: steps
          case _ => steps
        }
      }
    )

  val ciCommands = List(
    "project /",
    "clean",
    "test",
    "mimaReportBinaryIssues"
  )

}
