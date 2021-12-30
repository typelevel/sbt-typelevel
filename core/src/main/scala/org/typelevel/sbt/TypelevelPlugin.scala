package org.typelevel.sbt

import sbt._
import sbtghactions.GenerativePlugin
import sbtghactions.GitHubActionsPlugin

object TypelevelPlugin extends AutoPlugin {

  override def requires =
    TypelevelKernelPlugin && TypelevelSettingsPlugin && TypelevelVersioningPlugin && TypelevelMimaPlugin && TypelevelCiPlugin && GitHubActionsPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val tlFatalWarningsInCi = settingKey[Boolean](
      "Convert compiler warnings into errors under CI builds (default: true)")
  }

  import autoImport._
  import TypelevelSettingsPlugin.autoImport._
  import GenerativePlugin.autoImport._
  import GitHubActionsPlugin.autoImport._

  override def globalSettings = Seq(
    tlFatalWarningsInCi := true
  )

  override def buildSettings = Seq(
    Def.derive(tlFatalWarnings := (tlFatalWarningsInCi.value && githubIsWorkflowBuild.value)),
    githubWorkflowBuildMatrixExclusions ++= {
      for {
        // default scala is last in the list, default java first
        scala <- (ThisBuild / githubWorkflowScalaVersions).value.init
        java <- (ThisBuild / githubWorkflowJavaVersions).value.tail
      } yield MatrixExclude(Map("scala" -> scala, "java" -> java.render))
    }
  )

}
