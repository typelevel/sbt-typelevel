package org.typelevel.sbt

import sbt._
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
  import GitHubActionsPlugin.autoImport._

  override def globalSettings = Seq(
    tlFatalWarningsInCi := true
  )

  override def buildSettings = Seq(
    Def.derive(tlFatalWarnings := (tlFatalWarningsInCi.value && githubIsWorkflowBuild.value))
  )

}
