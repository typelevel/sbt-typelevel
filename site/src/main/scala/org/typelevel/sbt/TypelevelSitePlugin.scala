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
import laika.theme.config.Font
import laika.theme.config.FontDefinition
import laika.theme.config.FontStyle
import laika.theme.config.FontWeight
import mdoc.MdocPlugin
import org.typelevel.sbt.site._
import sbt._

import scala.annotation.nowarn
import Keys._
import MdocPlugin.autoImport._
import LaikaPlugin.autoImport._
import gha.GenerativePlugin
import GenerativePlugin.autoImport._
import TypelevelKernelPlugin._
import TypelevelKernelPlugin.autoImport._
import laika.io.model.FilePath

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
    implicit def tlLaikaThemeProviderOps(provider: ThemeProvider): LaikaThemeProviderOps =
      new site.LaikaThemeProviderOps(provider)
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
    laikaTheme := tlSiteHeliumConfig.value.build.extend(tlSiteHeliumExtensions.value),
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
      licenses.value.headOption,
      tlSiteRelatedProjects.value,
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
      // default fontPath and fonts taken from Laika:
      // instead of allowing for more fonts to be added, all the fonts must be respecified, to avoid "redundant embedding of unused fonts"
      // as a result, these things have to just be defined again if we want to change them, and we do
      val laikaFontPath = "laika/helium/fonts"
      val tlFontPath = "org/typelevel/sbt/site/fonts"

      val fonts = Seq(
        FontDefinition(
          Font
            .embedResource(s"$laikaFontPath/Lato/Lato-Regular.ttf")
            .webCSS("https://fonts.googleapis.com/css?family=Lato:400,700"),
          "Lato",
          FontWeight.Normal,
          FontStyle.Normal
        ),
        FontDefinition(
          Font.embedResource(s"$laikaFontPath/Lato/Lato-Italic.ttf"),
          "Lato",
          FontWeight.Normal,
          FontStyle.Italic
        ),
        FontDefinition(
          Font.embedResource(s"$laikaFontPath/Lato/Lato-Bold.ttf"),
          "Lato",
          FontWeight.Bold,
          FontStyle.Normal
        ),
        FontDefinition(
          Font.embedResource(s"$laikaFontPath/Lato/Lato-BoldItalic.ttf"),
          "Lato",
          FontWeight.Bold,
          FontStyle.Italic
        ),
        // Fira Code is the default used by Laika, but that has ligatures
        // Fira Mono is basically the same font, but without ligatures: yay!
        FontDefinition(
          Font
            .embedResource(s"$tlFontPath/FiraMono/FiraMono-Medium.ttf")
            .webCSS("https://fonts.googleapis.com/css?family=Fira+Mono:500"),
          "Fira Mono",
          FontWeight.Normal,
          FontStyle.Normal
        ),
        FontDefinition(
          Font.embedResource(s"$laikaFontPath/icofont/fonts/icofont.ttf"),
          "IcoFont",
          FontWeight.Normal,
          FontStyle.Normal
        )
      )

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
        .darkMode
        .disabled
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
            IconLink.external("https://fosstodon.org/@typelevel", TypelevelHeliumIcon.mastodon)
          )
        )
        .all
        .fontResources(fonts: _*)
        .all
        .fontFamilies(
          body = "Lato",
          headlines = "Lato",
          code = "Fira Mono" // this bit is changed from Laika defaults
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
            UseRef.Public("peaceiris", "actions-gh-pages", "v3.9.0"),
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
        scalas = List.empty,
        sbtStepPreamble = List.empty,
        javas = List(githubWorkflowJavaVersions.value.head),
        steps =
          githubWorkflowJobSetup.value.toList ++ tlSiteGenerate.value ++ tlSitePublish.value
      )
  )

  private def previewTask = Def
    .taskDyn {
      // inlined from https://github.com/planet42/Laika/blob/9022f6f37c9017f7612fa59398f246c8e8c42c3e/sbt/src/main/scala/laika/sbt/Tasks.scala#L192
      import cats.effect.IO
      import cats.effect.unsafe.implicits._
      import laika.sbt.Settings
      import laika.sbt.Tasks.generateAPI
      import laika.preview.{ServerBuilder, ServerConfig}

      val logger = streams.value.log
      logger.info("Initializing server...")

      def applyIf(
          flag: Boolean,
          f: ServerConfig => ServerConfig): ServerConfig => ServerConfig =
        if (flag) f else identity

      val previewConfig = laikaPreviewConfig.value
      val _ = generateAPI.value

      val applyFlags = applyIf(laikaIncludeEPUB.value, _.withEPUBDownloads)
        .andThen(applyIf(laikaIncludePDF.value, _.withPDFDownloads))
        .andThen(applyIf(
          laikaIncludeAPI.value,
          _.withAPIDirectory(FilePath.fromJavaFile(Settings.apiTargetDirectory.value))))
        .andThen(applyIf(previewConfig.isVerbose, _.verbose))

      val config = ServerConfig
        .defaults
        .withArtifactBasename(name.value)
        // .withHost(previewConfig.host)
        .withPort(previewConfig.port)
        .withPollInterval(previewConfig.pollInterval)

      val (_, cancel) = ServerBuilder[IO](Settings.parser.value, laikaInputs.value.delegate)
        .withLogger(s => IO(logger.info(s)))
        .withConfig(applyFlags(config))
        .build
        .allocated
        .unsafeRunSync()

      logger.info(s"Preview server started on port ${previewConfig.port}.")

      // watch but no-livereload b/c we don't need an mdoc server
      mdoc.toTask(" --watch --no-livereload").andFinally {
        logger.info(s"Shutting down preview server.")
        cancel.unsafeRunSync()
      }
    }
    // initial run of mdoc to bootstrap laikaPreview
    .dependsOn(mdoc.toTask(""))

}
