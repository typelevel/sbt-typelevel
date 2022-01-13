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

import sbt._, Keys._
import mdoc.MdocPlugin, MdocPlugin.autoImport._
import laika.sbt.LaikaPlugin, LaikaPlugin.autoImport._
import sbtghactions.GenerativePlugin, GenerativePlugin.autoImport._

object TypelevelMicrositePlugin extends AutoPlugin {

  override def requires = MdocPlugin && LaikaPlugin && GenerativePlugin

  override def buildSettings = Seq(
  )

  override def projectSettings = Seq(
    Laika / sourceDirectories := Seq(mdocOut.value),
    laikaExtensions ++= Seq(
      laika.markdown.github.GitHubFlavor,
      laika.parse.code.SyntaxHighlighting
    ),
    ThisBuild / githubWorkflowAddedJobs +=
      WorkflowJob(
        "publish-site",
        "Publish Site",
        scalas = List(crossScalaVersions.value.head),
        javas = githubWorkflowJavaVersions.value.toList,
        cond = Some("github.event_name != 'pull_request'"),
        needs = List("build"),
        steps = githubWorkflowJobSetup.value.toList ::: List(
          WorkflowStep.Sbt(List("docs/mdoc", "docs/laikaSite"), name = Some("Generate")),
          WorkflowStep.Use(
            UseRef.Public("peaceiris", "actions-gh-pages", "v3"),
            Map(
              "github_token" -> s"$${{ secrets.GITHUB_TOKEN }}",
              "publish_dir" -> s"${(Laika / target).value / "site"}",
              "publish_branch" -> "gh-pages"
            ),
            name = Some("Publish")
          )
        )
      )
  )

}
