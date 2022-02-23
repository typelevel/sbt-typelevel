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

import laika.helium.Helium
import laika.sbt.LaikaPlugin
import laika.theme.ThemeProvider
import mdoc.MdocPlugin
import org.typelevel.sbt.kernel.GitHelper
import org.typelevel.sbt.site._
import sbt._

import scala.annotation.nowarn

import Keys._
import MdocPlugin.autoImport._
import LaikaPlugin.autoImport._
import gha.GenerativePlugin
import GenerativePlugin.autoImport._

object TypelevelSitePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlSiteHeliumConfig = settingKey[Helium]("The Helium configuration")
    lazy val tlSiteTheme = settingKey[ThemeProvider]("The Typelevel Laika theme")
    lazy val tlSiteApiUrl = settingKey[Option[URL]]("URL to the API docs")
    lazy val tlSiteRelated =
      settingKey[Seq[(String, URL)]]("A list of related projects (default: cats)")

    lazy val tlSiteKeepFiles =
      settingKey[Boolean]("Whether to keep existing files when deploying site (default: true)")
    lazy val tlSiteGenerate = settingKey[Seq[WorkflowStep]](
      "A sequence of workflow steps which generates the site (default: [Sbt(List(\"tlSite\"))])")
    lazy val tlSitePublish = settingKey[Seq[WorkflowStep]](
      "A sequence of workflow steps which publishes the site (default: peaceiris/actions-gh-pages)")
    lazy val tlSitePublishBranch = settingKey[Option[String]](
      "The branch to publish the site from on every push. Set this to None if you only want to update the site on tag releases. (default: main)")
    lazy val tlSite = taskKey[Unit]("Generate the site (default: runs mdoc then laika)")

    val TypelevelProject = site.TypelevelProject
    implicit def tlLaikaThemeProviderOps(provider: ThemeProvider): LaikaThemeProviderOps =
      new site.LaikaThemeProviderOps(provider)
  }

  import autoImport._
  import TypelevelGitHubPlugin._

  override def requires =
    MdocPlugin && LaikaPlugin && TypelevelGitHubPlugin && GenerativePlugin && NoPublishPlugin

  override def buildSettings = Seq(
    tlSitePublishBranch := Some("main"),
    tlSiteApiUrl := None,
    tlSiteRelated := Seq(TypelevelProject.Cats),
    tlSiteKeepFiles := true,
    homepage := {
      gitHubUserRepo.value.map {
        case ("typelevel", repo) => url(s"https://typelevel.org/$repo")
        case (user, repo) => url(s"https://$user.github.io/$repo")
      }
    }
  )

  override def projectSettings = Seq(
    tlSite := Def
      .sequential(
        mdoc.toTask(""),
        laikaSite
      )
      .value: @nowarn("cat=other-pure-statement"),
    Laika / sourceDirectories := Seq(mdocOut.value),
    tlSiteTheme := {
      TypelevelHeliumTheme(
        gitHubUserRepo.value.map(_._2),
        developers.value,
        version.value,
        tlSiteApiUrl.value,
        scmInfo.value.map(_.browseUrl),
        licenses.value.headOption,
        tlSiteRelated.value
      )
    },
    tlSiteHeliumConfig := Helium.defaults,
    laikaTheme := {
      tlSiteHeliumConfig.value.build.extend(tlSiteTheme.value)
    },
    mdocVariables ++= Map(
      "VERSION" -> GitHelper
        .previousReleases(fromHead = true)
        .filterNot(_.isPrerelease)
        .headOption
        .fold(version.value)(_.toString),
      "SNAPSHOT_VERSION" -> version.value
    ),
    tlSiteGenerate := List(
      WorkflowStep.Sbt(
        List(s"${thisProject.value.id}/${tlSite.key.toString}"),
        name = Some("Generate site")
      )
    ),
    tlSitePublish := List(
      WorkflowStep.Use(
        UseRef.Public("peaceiris", "actions-gh-pages", "v3.8.0"),
        Map(
          "github_token" -> s"$${{ secrets.GITHUB_TOKEN }}",
          "publish_dir" -> (ThisBuild / baseDirectory)
            .value
            .toPath
            .toAbsolutePath
            .relativize((laikaSite / target).value.toPath)
            .toString,
          "keep_files" -> tlSiteKeepFiles.value.toString
        ),
        name = Some("Publish site"),
        cond = {
          val predicate = tlSitePublishBranch
            .value // Either publish from branch or on tags, not both
            .fold[RefPredicate](RefPredicate.StartsWith(Ref.Tag("v")))(b =>
              RefPredicate.Equals(Ref.Branch(b)))
          val publicationCond = GenerativePlugin.compileBranchPredicate("github.ref", predicate)
          Some(s"github.event_name != 'pull_request' && $publicationCond")
        }
      )
    ),
    ThisBuild / githubWorkflowAddedJobs +=
      WorkflowJob(
        "site",
        "Generate Site",
        scalas = List((ThisBuild / scalaVersion).value),
        javas = List(githubWorkflowJavaVersions.value.head),
        steps =
          githubWorkflowJobSetup.value.toList ++ tlSiteGenerate.value ++ tlSitePublish.value
      )
  )

}
