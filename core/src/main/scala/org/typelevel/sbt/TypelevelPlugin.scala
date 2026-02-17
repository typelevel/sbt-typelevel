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
import TypelevelKernelPlugin.autoImport._

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
    },
    GlobalScope / tlPrePRSteps ++= {
      val header = tlCiHeaderCheck.value
      val scalafmt = tlCiScalafmtCheck.value
      val javafmt = tlCiJavafmtCheck.value
      val scalafix = tlCiScalafixCheck.value

      List("githubWorkflowGenerate") ++
        List("+headerCreateAll").filter(_ => header) ++
        List("+scalafixAll").filter(_ => scalafix) ++
        List("+scalafmtAll", "scalafmtSbt").filter(_ => scalafmt) ++
        List("javafmtAll").filter(_ => javafmt)
    },
    GlobalScope / tlCommandAliases ++= {
      val header = tlCiHeaderCheck.value
      val scalafmt = tlCiScalafmtCheck.value
      val javafmt = tlCiJavafmtCheck.value

      val botHook = List("githubWorkflowGenerate") ++
        List("+headerCreateAll").filter(_ => header) ++
        List("javafmtAll").filter(_ => javafmt) ++
        List("+scalafmtAll", "scalafmtSbt").filter(_ => scalafmt)

      Map(
        "tlPrePrBotHook" -> botHook
      )
    }
  )

}
