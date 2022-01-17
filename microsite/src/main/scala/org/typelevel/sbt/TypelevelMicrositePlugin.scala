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
import gha.GenerativePlugin, GenerativePlugin.autoImport._
import scala.io.Source
import java.util.Base64

object TypelevelMicrositePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlHeliumConfig = settingKey[Helium]("The Helium configuration")
    lazy val tlApiUrl = settingKey[URL]("Url to the API scaladocs")
    lazy val tlSiteGenerate = settingKey[Seq[WorkflowStep]](
      "A sequence of steps which generates the site (default: [Sbt(List(\"tlSite\"))])")
    lazy val tlSitePublish = settingKey[Seq[WorkflowStep]](
      "A sequence of steps which publishes the site (default: peaceiris/actions-gh-pages)")
  }

  import autoImport._
  import TypelevelKernelPlugin.mkCommand

  override def requires = MdocPlugin && LaikaPlugin && GenerativePlugin

  override def projectSettings = Seq(
    Laika / sourceDirectories := Seq(mdocOut.value),
    laikaTheme := tlHeliumConfig.value.build,
    tlApiUrl := url(
      s"https://www.javadoc.io/doc/" +
        s"${projectID.value.organization}/" +
        s"${(RootProject(file(".")) / projectID).value.name}_${scalaBinaryVersion.value}"
    ),
    tlHeliumConfig := {
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
          navLinks = Seq(
            IconLink.external(
              tlApiUrl.value.toString,
              HeliumIcon.api,
              options = Styles("svg-link")
            ),
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
      WorkflowStep.Sbt(List("tlSite"), name = Some("Generate site"))
    ),
    tlSitePublish := List(
      WorkflowStep.Use(
        UseRef.Public("peaceiris", "actions-gh-pages", "v3"),
        Map(
          "github_token" -> s"$${{ secrets.GITHUB_TOKEN }}",
          "publish_dir" -> (ThisBuild / baseDirectory)
            .value
            .toPath
            .toAbsolutePath
            .relativize(((Laika / target).value / "site").toPath)
            .toString,
          "publish_branch" -> "gh-pages"
        ),
        name = Some("Publish site"),
        cond = {
          val publicationCondPre =
            githubWorkflowPublishTargetBranches
              .value
              .map(GenerativePlugin.compileBranchPredicate("github.ref", _))
              .mkString("(", " || ", ")")
          val publicationCond = githubWorkflowPublishCond.value match {
            case Some(cond) => publicationCondPre + " && (" + cond + ")"
            case None => publicationCondPre
          }
          Some(s"github.event_name != 'pull_request' && $publicationCond")
        }
      )
    ),
    ThisBuild / githubWorkflowAddedJobs +=
      WorkflowJob(
        "site",
        "Generate Site",
        scalas = List(crossScalaVersions.value.last),
        javas = List(githubWorkflowJavaVersions.value.head),
        steps =
          githubWorkflowJobSetup.value.toList ++ tlSiteGenerate.value ++ tlSitePublish.value
      )
  ) ++ addCommandAlias("tlSite", mkCommand(List("mdoc", "laikaSite")))

  private def getSvgLogo: String = {
    val src = Source.fromURL(getClass.getResource("/logo.svg"))
    try {
      Base64.getEncoder().encodeToString(src.mkString.getBytes)
    } finally {
      src.close()
    }
  }

}
