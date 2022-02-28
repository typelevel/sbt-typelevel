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
import laika.ast._
import laika.ast.LengthUnit._
import laika.sbt.LaikaPlugin, LaikaPlugin.autoImport._
import laika.helium.Helium
import laika.helium.config.{HeliumIcon, IconLink, ImageLink}
import org.typelevel.sbt.kernel.GitHelper
import gha.GenerativePlugin, GenerativePlugin.autoImport._
import scala.io.Source
import java.util.Base64
import scala.annotation.nowarn

object TypelevelSitePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlSiteHeliumConfig = settingKey[Helium]("The Helium configuration")
    lazy val tlSiteApiUrl = settingKey[Option[URL]]("URL to the API docs")
    lazy val tlSiteKeepFiles =
      settingKey[Boolean]("Whether to keep existing files when deploying site (default: true)")
    lazy val tlSiteGenerate = settingKey[Seq[WorkflowStep]](
      "A sequence of workflow steps which generates the site (default: [Sbt(List(\"tlSite\"))])")
    lazy val tlSitePublish = settingKey[Seq[WorkflowStep]](
      "A sequence of workflow steps which publishes the site (default: peaceiris/actions-gh-pages)")
    lazy val tlSitePublishBranch = settingKey[Option[String]](
      "The branch to publish the site from on every push. Set this to None if you only want to update the site on tag releases. (default: main)")
    lazy val tlSite = taskKey[Unit]("Generate the site (default: runs mdoc then laika)")
    lazy val tlSitePreview = taskKey[Unit](
      "Start a live-reload preview server (combines mdoc --watch with laikaPreview)")
  }

  import autoImport._
  import TypelevelGitHubPlugin._

  override def requires =
    MdocPlugin && LaikaPlugin && TypelevelGitHubPlugin && GenerativePlugin && NoPublishPlugin

  override def buildSettings = Seq(
    tlSitePublishBranch := Some("main"),
    tlSiteApiUrl := None,
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
    laikaTheme := tlSiteHeliumConfig.value.build,
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
        .layout(
          contentWidth = px(860),
          navigationWidth = px(275),
          topBarHeight = px(50),
          defaultBlockSpacing = px(10),
          defaultLineHeight = 1.5,
          anchorPlacement = laika.helium.config.AnchorPlacement.Right
        )
        // .site
        // .favIcons( // TODO broken?
        //   Favicon.external("https://typelevel.org/img/favicon.png", "32x32", "image/png")
        // )
        .site
        .topNavigationBar(
          homeLink = ImageLink.external(
            "https://typelevel.org",
            Image.external(s"data:image/svg+xml;base64,$getSvgLogo")
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
    laikaExtensions ++= Seq(
      laika.markdown.github.GitHubFlavor,
      laika.parse.code.SyntaxHighlighting
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

  private def getSvgLogo: String = {
    val src = Source.fromURL(getClass.getResource("/logo.svg"))
    try {
      Base64.getEncoder().encodeToString(src.mkString.getBytes)
    } finally {
      src.close()
    }
  }

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
        .andThen(
          applyIf(laikaIncludeAPI.value, _.withAPIDirectory(Settings.apiTargetDirectory.value)))
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
