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

import laika.ast.LengthUnit.*
import laika.ast.*
import laika.helium.Helium
import laika.helium.config.{
  Favicon,
  HeliumIcon,
  IconLink,
  ImageLink,
  TextLink,
  ThemeNavigationSection
}
import laika.sbt.{LaikaPlugin, Tasks}
import laika.theme.ThemeProvider
import mdoc.MdocPlugin
import org.typelevel.sbt.site.*
import sbt.*

import scala.annotation.nowarn
import Keys.*
import MdocPlugin.autoImport.*
import LaikaPlugin.autoImport.*
import gha.GenerativePlugin
import GenerativePlugin.autoImport.*
import TypelevelKernelPlugin.*
import TypelevelKernelPlugin.autoImport.*
import cats.data.NonEmptyList

object TypelevelSitePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlSiteHeliumConfig = settingKey[Helium]("The Typelevel Helium configuration")
    lazy val tlSiteHeliumExtensions =
      settingKey[ThemeProvider]("The Typelevel Helium extensions")
    lazy val tlSiteApiUrl = settingKey[Option[URL]]("URL to the API docs")
    lazy val tlSiteApiModule =
      settingKey[Option[ModuleID]]("The module that publishes API docs")
    lazy val tlSiteApiPackage = settingKey[Option[String]](
      "The top-level package for your API docs (e.g. org.typlevel.sbt)")
    lazy val tlSiteRelatedProjects =
      settingKey[Seq[(String, URL)]]("A list of related projects (default: cats)")

    lazy val tlSiteKeepFiles =
      settingKey[Boolean]("Whether to keep existing files when deploying site (default: true)")
    lazy val tlSiteGenerate = settingKey[Seq[WorkflowStep]](
      "A sequence of workflow steps which generates the site (default: [Sbt(List(\"tlSite\"))])")
    lazy val tlSitePublish = settingKey[Seq[WorkflowStep]](
      "A sequence of workflow steps which publishes the site (default: peaceiris/actions-gh-pages)")
    lazy val tlSitePublishBranch = settingKey[Option[String]](
      "The branch to publish the site from on every push. Set this to None if you only want to update the site on tag releases. (default: main)")
    lazy val tlSitePublishTags = settingKey[Boolean](
      "Defines whether the site should be published on tag releases. Note on setting this to true requires the 'tlSitePublishBranch' setting to be set to None. (default: false)")
    lazy val tlSite = taskKey[Unit]("Generate the site (default: runs mdoc then laika)")
    lazy val tlSitePreview = taskKey[Unit](
      "Start a live-reload preview server (combines mdoc --watch with laikaPreview)")

    val TypelevelProject = site.TypelevelProject
  }

  import autoImport._
  import TypelevelGitHubPlugin._

  override def requires =
    MdocPlugin && LaikaPlugin && TypelevelGitHubPlugin && GenerativePlugin && NoPublishPlugin

  override def globalSettings = Seq(
    tlSiteApiModule := None
  )

  override def buildSettings = Seq(
    tlSitePublishBranch := Some("main"),
    tlSitePublishTags := tlSitePublishBranch.value.isEmpty,
    tlSiteApiUrl := None,
    tlSiteApiPackage := None,
    tlSiteRelatedProjects := Seq(TypelevelProject.Cats),
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
    tlSitePreview := previewTask.value,
    Laika / sourceDirectories := Seq(mdocOut.value),
    laikaTheme := tlSiteHeliumConfig.value.build.extendWith(tlSiteHeliumExtensions.value),
    mdocVariables := {
      mdocVariables.value ++
        Map(
          "VERSION" -> currentRelease.value.getOrElse(version.value),
          "PRERELEASE_VERSION" -> currentPreRelease.value.getOrElse(version.value),
          "SNAPSHOT_VERSION" -> version.value
        ) ++
        tlSiteApiUrl.value.map("API_URL" -> _.toString).toMap
    },
    tlSiteHeliumExtensions := TypelevelHeliumExtensions(
      tlIsScala3.value,
      tlSiteApiUrl.value
    ),
    tlSiteApiUrl := {
      val javadocioUrl = for {
        moduleId <- (ThisProject / tlSiteApiModule).value
        cross <- CrossVersion(
          moduleId.crossVersion,
          scalaVersion.value,
          scalaBinaryVersion.value
        )
        version <- currentRelease.value
      } yield {
        val o = moduleId.organization
        val n = cross(moduleId.name)
        val v = version
        val p = tlSiteApiPackage.value.fold("")(_.replace('.', '/') + "/index.html")
        url(s"https://www.javadoc.io/doc/$o/$n/$v/$p")
      }
      lazy val fallbackUrl = for {
        moduleId <- (ThisProject / tlSiteApiModule).value
        apiURL <- moduleId.extraAttributes.get("e:info.apiURL")
      } yield url(apiURL)

      tlSiteApiUrl.value.orElse(javadocioUrl).orElse(fallbackUrl)
    },
    tlSiteHeliumConfig := {
      val title = gitHubUserRepo.value.map(_._2)
      val footerSpans = title.fold(Seq[Span]()) { title =>
        val licensePhrase = licenses.value.headOption.fold("") {
          case (url, name) => s""" distributed under the <a href="$url">$name</a> license"""
        }
        Seq(TemplateString(
          s"""$title is a <a href="https://typelevel.org/">Typelevel</a> project$licensePhrase."""
        ))
      }
      val relatedProjects =
        NonEmptyList.fromList(tlSiteRelatedProjects.value.toList).toList.map { projects =>
          ThemeNavigationSection(
            "Related Projects",
            projects.map { case (name, url) => TextLink.external(url.toString, name) })
        }
      Helium
        .defaults
        .site
        .metadata(
          title = title,
          authors = developers.value.map(_.name),
          language = Some("en"),
          version = Some(version.value.toString)
        )
        .site
        .layout(
          topBarHeight = px(50)
        )
        .site
        .darkMode
        .disabled
        .site
        .favIcons(
          Favicon.external("https://typelevel.org/img/favicon.png", "32x32", "image/png")
        )
        .site
        .footer(footerSpans: _*)
        .site
        .mainNavigation(appendLinks = relatedProjects)
        .site
        .topNavigationBar(
          homeLink = ImageLink.external(
            "https://typelevel.org",
            Image.external(s"https://typelevel.org/img/logo.svg")
          ),
          navLinks = tlSiteApiUrl.value.toList.map { url =>
            IconLink.external(url.toString, HeliumIcon.api)
          } ++ List(
            IconLink.external(
              scmInfo.value.fold("https://github.com/typelevel")(_.browseUrl.toString),
              HeliumIcon.github),
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
    tlSitePublish := {
      def publishSiteWorkflowStep(publishPredicate: RefPredicate) =
        List(
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
              val predicate = publishPredicate
              val publicationCond =
                GenerativePlugin.compileBranchPredicate("github.ref", predicate)
              Some(s"github.event_name != 'pull_request' && $publicationCond")
            }
          )
        )

      val tlSitePublishTagsV = tlSitePublishTags.value
      val tlSitePublishBranchV = tlSitePublishBranch.value

      (tlSitePublishTagsV, tlSitePublishBranchV) match {
        case (true, Some(_)) =>
          sys.error(
            s"'tlSitePublishTags' setting is set to 'true' which conflicts with 'tlSitePublishBranch' which is non-empty. " +
              s"Site publishing is available from tags or a particular branch, not from both.")

        case (true, None) =>
          publishSiteWorkflowStep(RefPredicate.StartsWith(Ref.Tag("v")))

        case (false, Some(branch)) =>
          publishSiteWorkflowStep(RefPredicate.Equals(Ref.Branch(branch)))

        case (false, None) =>
          List.empty
      }
    },
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

  private def previewTask = Def
    .taskDyn {
      import cats.effect.unsafe.implicits._

      val logger = streams.value.log
      logger.info("Initializing server...")

      val (_, cancel) = Tasks.buildPreviewServer.value.allocated.unsafeRunSync()

      logger.info(s"Preview server started on port ${laikaPreviewConfig.value.port}.")

      // watch but no-livereload b/c we don't need an mdoc server
      mdoc.toTask(" --watch --no-livereload").andFinally {
        logger.info(s"Shutting down preview server.")
        cancel.unsafeRunSync()
      }
    }
    // initial run of mdoc to bootstrap laikaPreview
    .dependsOn(mdoc.toTask(""))

}
