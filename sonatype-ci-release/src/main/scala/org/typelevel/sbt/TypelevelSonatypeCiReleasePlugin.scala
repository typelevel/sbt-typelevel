package org.typelevel.sbt

import sbt._
import sbtghactions.GenerativePlugin
import sbtghactions.GitHubActionsPlugin
import sbtghactions.GenerativePlugin.autoImport._

object TypelevelSonatypeCiReleasePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlCiReleaseSnapshots = settingKey[Boolean](
      "Controls whether or not snapshots should be released (default: false)")
    lazy val tlCiReleaseBranches =
      settingKey[Seq[String]]("The branches in your repository to release from (default: [])")
  }

  import autoImport._

  override def requires = TypelevelSonatypePlugin && GitHubActionsPlugin &&
    GenerativePlugin

  override def trigger = noTrigger

  override def globalSettings =
    Seq(tlCiReleaseSnapshots := false, tlCiReleaseBranches := Seq())

  override def buildSettings = Seq(
    githubWorkflowEnv ++= Map(
      "SONATYPE_USERNAME" -> s"$${{ secrets.SONATYPE_USERNAME }}",
      "SONATYPE_PASSWORD" -> s"$${{ secrets.SONATYPE_PASSWORD }}"
    ),
    githubWorkflowPublishTargetBranches := {
      val seed =
        if (tlCiReleaseSnapshots.value)
          tlCiReleaseBranches.value.map(b => RefPredicate.Equals(Ref.Branch(b)))
        else
          Seq.empty

      RefPredicate.StartsWith(Ref.Tag("v")) +: seed
    },
    githubWorkflowTargetTags += "v*",
    githubWorkflowPublish := Seq(
      WorkflowStep.Sbt(
        List("release"),
        cond = Some( // NEVER release a tag on a non-tag workflow run
          "(startsWith(github.ref, 'refs/tags/v') && github.ref_type == 'tag') || (!startsWith(github.ref, 'refs/tags/v') && github.ref_type != 'tag')")
      )
    )
  )
}
