/*
 * Copyright 2021 Typelevel
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
