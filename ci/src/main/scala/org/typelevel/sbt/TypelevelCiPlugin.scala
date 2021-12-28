package org.typelevel.sbt

import sbt._, Keys._
import sbtghactions.GenerativePlugin
import sbtghactions.GitHubActionsPlugin
import sbtghactions.GenerativePlugin.autoImport._
import com.typesafe.tools.mima.plugin.MimaPlugin

object TypelevelCiPlugin extends AutoPlugin {

  override def requires = GitHubActionsPlugin && GenerativePlugin && MimaPlugin
  override def trigger = allRequirements

  override def buildSettings =
    addCommandAlias(
      "ci",
      List(
        "project /",
        "clean",
        "test",
        "mimaReportBinaryIssues"
      ).mkString("; ", "; ", "")
    ) ++ addCommandAlias("releaseLocal", "; reload; project /; +publishLocal") ++ Seq(
      githubWorkflowPublishTargetBranches := Seq(),
      githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("ci"))),
      githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"))
    )

}
