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
        name = Some("Check binary compatibility"),
        cond = Some(primaryJavaCond.value)
      ),
      WorkflowStep.Sbt(
        List("doc"),
        name = Some("Generate API documentation"),
        cond = Some(primaryJavaCond.value)
      )
    ),
    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8")),
    GlobalScope / Keys.onLoad := {
      val oses = githubWorkflowOSes.value.toList
      val javas = githubWorkflowJavaVersions.value.toList
      val scalas = githubWorkflowScalaVersions.value.toList
      val additions = githubWorkflowBuildMatrixAdditions.value
      val inclusions = githubWorkflowBuildMatrixInclusions.value.toList
      val exclusions = githubWorkflowBuildMatrixExclusions.value.toList
      val stepPreamble = githubWorkflowBuildSbtStepPreamble.value.toList

      (GlobalScope / Keys.onLoad).value.compose { (state: State) =>
        addCiAlias(
          state,
          oses,
          javas,
          scalas,
          additions,
          inclusions,
          exclusions,
          stepPreamble,
          githubWorkflowBuild.value
        )
      }
    },
    GlobalScope / Keys.onUnload := {
      (GlobalScope / Keys.onUnload)
        .value
        .compose((state: State) => BasicCommands.removeAlias(state, "ci"))
    }
  )

  private val primaryJavaCond = Def.setting {
    val java = githubWorkflowJavaVersions.value.head
    s"matrix.java == '${java.render}'"
  }

  private def addCiAlias(
      state: State,
      oses: List[String],
      javaVersions: List[JavaSpec],
      scalaVersions: List[String],
      matrixAdditions: Map[String, List[String]],
      matrixInclusions: List[MatrixInclude],
      matrixExclusions: List[MatrixExclude],
      sbtStepPreamble: List[String],
      workflowSteps: Seq[WorkflowStep]) = {

    val buildMatrix = GenerativePlugin.expandMatrix(
      // Cannot meaningfully iterate OS or Java version here
      oses = oses.take(1),
      javas = javaVersions.take(1),
      scalas = scalaVersions,
      matrixAdds = matrixAdditions,
      includes = matrixInclusions,
      excludes = matrixExclusions
    )

    val keys = "os" :: "scala" :: "java" :: matrixAdditions.keys.toList.sorted

    val commands = for {
      matrixRow <- buildMatrix
      matrixValues = keys.zip(matrixRow).toMap
      step <- workflowSteps.collect {
        case sbt: WorkflowStep.Sbt if matchingCondition(sbt, matrixValues) => sbt
      }
      command <- sbtStepPreamble ++ step.commands
    } yield replaceMatrixVars(command, matrixValues)

    val commandAlias = TypelevelKernelPlugin.mkCommand(commands)

    BasicCommands.addAlias(
      state,
      "ci",
      commandAlias
    )
  }

  private def matchingCondition(
      command: WorkflowStep.Sbt,
      matrixValues: Map[String, String]) = {
    // For all matrix values
    matrixValues.forall {
      case (k, v) =>
        val renderedCond = s"matrix.$k == '$v'"

        command.cond.forall { cond =>
          // If the condition starts with this matrix variable, whole condition must be equal
          if (cond.startsWith(s"matrix.$k")) cond == renderedCond else true
        }
    }
  }

  private def replaceMatrixVars(command: String, matrixValues: Map[String, String]): String =
    matrixValues.foldLeft(command) {
      case (cmd, (matrixVar, matrixVarValue)) =>
        cmd.replaceAll(s"\\$$\\{\\{ matrix.${matrixVar} \\}\\}", matrixVarValue)
    }
}
