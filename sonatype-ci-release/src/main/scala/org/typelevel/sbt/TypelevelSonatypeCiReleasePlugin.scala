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

import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._
import org.typelevel.sbt.gha.GitHubActionsPlugin
import sbt.Keys._
import sbt._

object TypelevelSonatypeCiReleasePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlCiReleaseTags = settingKey[Boolean](
      "Controls whether or not v-prefixed tags should be released from CI (default true)")
    lazy val tlCiReleaseBranches = settingKey[Seq[String]](
      "The branches in your repository to release from in CI on every push. Depending on your versioning scheme, they will be either snapshots or (hash) releases. Leave this empty if you only want CI releases for tags. (default: [])")
    lazy val tlCiReleaseStepSummaryTableInfo = settingKey[Map[String, String]](
      "Key-value set of information that will be rendered in a table in the job summary (default: [Release version -> ThisBuild/version])")
    lazy val tlCiAddResolverInfoToSummary = settingKey[Boolean](
      "If true adds the resolver url and instructions to the job summary (default: true)")
  }

  import autoImport._

  override def requires = TypelevelSonatypePlugin && GitHubActionsPlugin &&
    GenerativePlugin

  override def trigger = allRequirements

  override def globalSettings =
    Seq(tlCiReleaseTags := true, tlCiReleaseBranches := Seq())

  override def buildSettings = Seq(
    githubWorkflowEnv ++= List(
      "SONATYPE_USERNAME",
      "SONATYPE_PASSWORD",
      "SONATYPE_CREDENTIAL_HOST").map(k => k -> s"$${{ secrets.$k }}").toMap,
    githubWorkflowPublishTargetBranches := {
      val branches =
        tlCiReleaseBranches.value.map(b => RefPredicate.Equals(Ref.Branch(b)))

      val tags =
        if (tlCiReleaseTags.value)
          Seq(RefPredicate.StartsWith(Ref.Tag("v")))
        else
          Seq.empty

      tags ++ branches
    },
    tlCiReleaseStepSummaryTableInfo := {
      val map: Map[String, String] = Map("Release version" -> (ThisBuild / version).value)
      (ThisBuild / apiURL).value.map(_.toString).fold(map)(r => map + ("Api URL" -> r))
    },
    tlCiAddResolverInfoToSummary := true,
    githubWorkflowTargetTags += "v*",
    githubWorkflowPublish := Seq(
      WorkflowStep.Sbt(List("tlRelease"), name = Some("Publish"))
    )
  )

  private def renderSummaryTable(results: Map[String, String]): String =
    results
      .toList
      .map { case (k, v) => s"| ${k} | ${v} |" }
      .mkString(s"| Build Result | Value |\n| -: | :- |\n", "\n", "\n\n")

  override def projectSettings: Seq[Setting[_]] = Seq(
    publish := {
      val table: Map[String, String] = tlCiReleaseStepSummaryTableInfo.value
      val projectName: String = name.value
      val maybeMavenResolverUrl: Option[(String, String)] =
        (ThisBuild / publishTo).value.collect {
          case x: MavenRepo => (x.name, x.root)
          case x: MavenRepository => (x.name, x.root)
        }

      val header: String = s"### ${projectName} Publication Summary\n"

      val textToRender: String =
        maybeMavenResolverUrl
          .filter(_ => tlCiAddResolverInfoToSummary.value)
          .fold(renderSummaryTable(table)) {
            case (n, u) =>
              val newTable: Map[String, String] = table + ("Resolver" -> s""""${n}" -> ${u}""")

              val instructions: String =
                s"To configure your build to use this published version set\n" +
                  s"""`resolvers += Resolver.url("${n}", url("${u}"))`\n\n"""

              renderSummaryTable(newTable) + instructions
          }

      GitHubActionsPlugin.appendtoStepSummary(header + textToRender)

      publish.value
    }
  )
}
