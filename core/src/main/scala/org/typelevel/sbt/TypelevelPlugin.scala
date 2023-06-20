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

import de.heikoseeberger.sbtheader.HeaderPlugin
import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GitHubActionsPlugin
import sbt._

import Keys._

/**
 * The [[TypelevelPlugin]] brings together the [[TypelevelCiReleasePlugin]] and the
 * [[TypelevelSettingsPlugin]]
 */
object TypelevelPlugin extends AutoPlugin {

  override def requires =
    TypelevelKernelPlugin &&
      TypelevelSettingsPlugin &&
      TypelevelCiReleasePlugin &&
      GitHubActionsPlugin &&
      HeaderPlugin

  override def trigger = allRequirements

  object autoImport {
    @deprecated("No longer has an effect. Use `tlFatalWarnings` instead.", "0.5.0")
    lazy val tlFatalWarningsInCi = settingKey[Boolean](
      "Convert compiler warnings into errors under CI builds (default: true)")
  }

  import TypelevelKernelPlugin.mkCommand
  import TypelevelCiPlugin.autoImport._
  import TypelevelSettingsPlugin.autoImport._
  import TypelevelSonatypeCiReleasePlugin.autoImport._
  import GenerativePlugin.autoImport._
  import GitHubActionsPlugin.autoImport._

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
    licenses += License.Apache2,
    tlCiHeaderCheck := true,
    tlCiScalafmtCheck := true,
    tlCiReleaseBranches := Seq("main"),
    Def.derive(tlFatalWarnings := githubIsWorkflowBuild.value),
    githubWorkflowJavaVersions := {
      Seq(JavaSpec.temurin(tlJdkRelease.value.getOrElse(8).toString))
    },
    githubWorkflowBuildMatrixExclusions ++= {
      val defaultScala = (ThisBuild / scalaVersion).value
      for {
        scala <- githubWorkflowScalaVersions.value.filterNot(defaultScala.startsWith(_))
        java <- githubWorkflowJavaVersions.value.tail // default java is head
      } yield MatrixExclude(Map("scala" -> scala, "java" -> java.render))
    }
  ) ++ addPrePRCommandAlias ++ addTlPrePRBotHookCommandAlias

  // partially re-implemnents addCommandAlias
  // this is so we can use the value of other settings to generate command
  private def addPrePRCommandAlias: Seq[Setting[_]] = Seq(
    GlobalScope / onLoad := {
      val header = tlCiHeaderCheck.value
      val scalafmt = tlCiScalafmtCheck.value
      val scalafix = tlCiScalafixCheck.value

      (GlobalScope / Keys.onLoad).value.compose { (state: State) =>
        val command = mkCommand(
          List("project /", "githubWorkflowGenerate") ++
            List("+headerCreateAll").filter(_ => header) ++
            List("+scalafmtAll", "scalafmtSbt").filter(_ => scalafmt) ++
            List("+scalafixAll").filter(_ => scalafix)
        )
        BasicCommands.addAlias(state, "prePR", command)
      }
    },
    GlobalScope / Keys.onUnload := {
      (GlobalScope / Keys.onUnload)
        .value
        .compose((state: State) => BasicCommands.removeAlias(state, "prePR"))
    }
  )

  private def addTlPrePRBotHookCommandAlias: Seq[Setting[_]] = Seq(
    GlobalScope / onLoad := {
      val header = tlCiHeaderCheck.value
      val scalafmt = tlCiScalafmtCheck.value

      (GlobalScope / Keys.onLoad).value.compose { (state: State) =>
        val command = mkCommand(
          List("githubWorkflowGenerate") ++
            List("+headerCreateAll").filter(_ => header) ++
            List("+scalafmtAll", "scalafmtSbt").filter(_ => scalafmt)
        )
        BasicCommands.addAlias(state, "tlPrePrBotHook", command)
      }
    },
    GlobalScope / Keys.onUnload := {
      (GlobalScope / Keys.onUnload)
        .value
        .compose((state: State) => BasicCommands.removeAlias(state, "tlPrePrBotHook"))
    }
  )

}
