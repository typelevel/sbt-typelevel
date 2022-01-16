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
import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GitHubActionsPlugin
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._
import com.typesafe.tools.mima.plugin.MimaPlugin

object TypelevelCiPlugin extends AutoPlugin {

  override def requires = GitHubActionsPlugin && GenerativePlugin && MimaPlugin
  override def trigger = allRequirements

  object autoImport {
    def tlCrossRootProject: CrossRootProject = CrossRootProject()
  }

  override def buildSettings = Seq(
    githubWorkflowPublishTargetBranches := Seq(),
    githubWorkflowBuild := Seq(
      WorkflowStep.Sbt(List("test"), name = Some("Test")),
      WorkflowStep.Sbt(
        List("mimaReportBinaryIssues"),
        name = Some("Check binary compatibility")
      ),
      WorkflowStep.Sbt(List("doc"), name = Some("Generate API documentation"))
    ),
    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"))
  )

}
