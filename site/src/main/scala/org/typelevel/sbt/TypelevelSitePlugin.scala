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
import laika.sbt.LaikaPlugin.autoImport._
import laika.sbt.Tasks
import laika.theme.ThemeProvider
import mdoc.MdocPlugin
import mdoc.MdocPlugin.autoImport._
import org.typelevel.sbt.TypelevelKernelPlugin._
import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._
import org.typelevel.sbt.site._
import sbt.Keys._
import sbt._

import scala.annotation.nowarn

object TypelevelSitePlugin extends AutoPlugin {

  object autoImport {

    @deprecated("Use tlSiteHelium", "0.5.0")
    lazy val tlSiteHeliumConfig = settingKey[Helium]("The Typelevel Helium configuration")
    @deprecated("Use tlSiteHelium", "0.5.0")
    lazy val tlSiteHeliumExtensions =
      settingKey[ThemeProvider]("The Typelevel Helium extensions")
    @deprecated("Use .site.mainNavigation(appendLinks = ...) in tlSiteHelium", "0.5.0")
    lazy val tlSiteRelatedProjects =
      settingKey[Seq[(String, URL)]]("A list of related projects (default: empty)")

    lazy val tlSiteHelium = settingKey[Helium]("The Helium theme configuration and extensions")
    lazy val tlSiteIsTypelevelProject =
      settingKey[Option[TypelevelProject]](
        "Indicates whether the generated site should be pre-populated with UI elements specific to Typelevel Organization or Affiliate projects (default: None)")

    lazy val tlSiteApiUrl = settingKey[Option[URL]]("URL to the API docs")
    lazy val tlSiteApiModule =
      settingKey[Option[ModuleID]]("The module that publishes API docs")
    lazy val tlSiteApiPackage = settingKey[Option[String]](
      "The top-level package for your API docs (e.g. org.typlevel.sbt)")

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

    type TypelevelProject = site.TypelevelProject
    val TypelevelProject = site.TypelevelProject
  }

  import autoImport._
  import TypelevelGitHubPlugin._

  override def requires =
    MdocPlugin && LaikaPlugin && TypelevelGitHubPlugin && GenerativePlugin

  override def globalSettings = Seq(
    tlSiteApiModule := None
  )

  @nowarn("cat=deprecation")
  override def buildSettings = Seq(
    tlSitePublishBranch := Some("main"),
    tlSitePublishTags := tlSitePublishBranch.value.isEmpty,
    tlSiteApiUrl := None,
    tlSiteApiPackage := None,
    tlSiteRelatedProjects := Nil,
    tlSiteKeepFiles := true,
    homepage := {
      gitHubUserRepo.value.map {
        case ("typelevel", repo) => url(s"https://typelevel.org/$repo")
        case (user, repo) => url(s"https://$user.github.io/$repo")
      }
    }
  )

  @nowarn("cat=deprecation")
  override def projectSettings = Seq(
    tlSite := Def
      .sequential(
        mdoc.toTask(""),
        laikaSite
      )
      .value: @nowarn("cat=other-pure-statement"),
    tlSitePreview := previewTask.value,
    Laika / sourceDirectories := Seq(mdocOut.value),
    Compile / packageDoc / mappings := {
      (laikaSite / mappings).dependsOn(tlSite).value
    },
    SettingKey[Set[ModuleID]]("mimaPreviousArtifacts") := Set.empty,
    laikaTheme := tlSiteHelium.value.build,
    mdocVariables := {
      mdocVariables.value ++
        Map(
          "VERSION" -> currentRelease.value.getOrElse(version.value),
          "PRERELEASE_VERSION" -> currentPreRelease.value.getOrElse(version.value),
          "SNAPSHOT_VERSION" -> version.value
        ) ++
        tlSiteApiUrl.value.map("API_URL" -> _.toString).toMap
    },
    tlSiteIsTypelevelProject := {
      if (organization.value == "org.typelevel")
        Some(TypelevelProject.Organization)
      else
        None
    },
    tlSiteHeliumConfig := TypelevelSiteSettings.defaults.value,
    tlSiteHeliumExtensions := GenericSiteSettings.themeExtensions.value,
    tlSiteHelium := {
      if (tlSiteIsTypelevelProject.value.isDefined) tlSiteHeliumConfig.value
      else GenericSiteSettings.defaults.value
    },
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
            UseRef.Public("peaceiris", "actions-gh-pages", "v3.9.3"),
            Map(
              "github_token" -> s"$${{ secrets.GITHUB_TOKEN }}",
              "publish_dir" -> {
                val path = (ThisBuild / baseDirectory)
                  .value
                  .toPath
                  .toAbsolutePath
                  .relativize((laikaSite / target).value.toPath)

                // os-independent path rendering ...
                (0 until path.getNameCount).map(path.getName).mkString("/")
              },
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
    // avoid depending directly on sbt-typelevel-ci
    ThisBuild / SettingKey[Seq[ProjectRef]]("tlCiDependencyIgnoreModules") := {
      val old = (ThisBuild / SettingKey[Seq[ProjectRef]]("tlCiDependencyIgnoreModules")).?.value
      old.getOrElse(Seq.empty) :+ thisProjectRef.value
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
      import cats.effect.unsafe.implicits._

      val logger = streams.value.log
      logger.info("Initializing server...")

      val (_, cancel) = Tasks.buildPreviewServer.value.allocated.unsafeRunSync()

      logger.info(s"Preview server started on port ${laikaPreviewConfig.value.port}.")

      // watch but no-livereload b/c we don't need an mdoc server
      mdoc.toTask(" --watch --no-livereload").andFinally {
        logger.info(s"Shutting down preview server...")
        cancel.unsafeRunSync()
      }
    }
    // initial run of mdoc to bootstrap laikaPreview
    .dependsOn(mdoc.toTask(""))

}
