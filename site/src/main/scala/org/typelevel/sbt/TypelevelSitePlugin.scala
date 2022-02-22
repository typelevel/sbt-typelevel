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

import laika.ast.LengthUnit._
import laika.ast._
import laika.helium.Helium
import laika.helium.config.Favicon
import laika.helium.config.HeliumIcon
import laika.helium.config.IconLink
import laika.helium.config.ImageLink
import laika.sbt.LaikaPlugin
import laika.theme.ThemeProvider
import mdoc.MdocPlugin
import org.typelevel.sbt.kernel.GitHelper
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

    val TypelevelProject = org.typelevel.sbt.TypelevelProject
    implicit def tlLaikaThemeProviderOps(provider: ThemeProvider): LaikaThemeProviderOps =
      new LaikaThemeProviderOps(provider)
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
    laikaTheme := tlSiteHeliumConfig
      .value
      .build
      .extend(TypelevelHeliumExtensions(licenses.value.headOption, tlSiteRelated.value)),
    mdocVariables ++= Map(
      "VERSION" -> GitHelper
        .previousReleases(fromHead = true)
        .filterNot(_.isPrerelease)
        .headOption
        .fold(version.value)(_.toString),
      "SNAPSHOT_VERSION" -> version.value
    ),
    tlSiteHeliumConfig := {
      Helium
        .defaults
        .site
        .metadata(
          title = gitHubUserRepo.value.map(_._2),
          authors = developers.value.map(_.name),
          language = Some("en"),
          version = Some(version.value.toString)
        )
        .site
        .layout(
          contentWidth = px(860),
          navigationWidth = px(275),
          topBarHeight = px(50),
          defaultBlockSpacing = px(10),
          defaultLineHeight = 1.5,
          anchorPlacement = laika.helium.config.AnchorPlacement.Right
        )
        .site
        .favIcons(
          Favicon.external("https://typelevel.org/img/favicon.png", "32x32", "image/png")
        )
        .site
        .topNavigationBar(
          homeLink = ImageLink.external(
            "https://typelevel.org",
            Image.external(s"https://typelevel.org/img/logo.svg")
          ),
          navLinks = tlSiteApiUrl.value.toList.map { url =>
            IconLink.external(
              url.toString,
              HeliumIcon.api,
              options = Styles("svg-link")
            )
          } ++ List(
            IconLink.external(
              scmInfo.value.fold("https://github.com/typelevel")(_.browseUrl.toString),
              HeliumIcon.github,
              options = Styles("svg-link")),
            IconLink.external("https://discord.gg/XF3CXcMzqD", HeliumIcon.chat),
            IconLink.external("https://twitter.com/typelevel", HeliumIcon.twitter)
          )
        )
    },
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
