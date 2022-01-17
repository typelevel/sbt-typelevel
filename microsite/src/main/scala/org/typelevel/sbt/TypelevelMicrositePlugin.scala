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
import laika.helium.config.{
  Favicon,
  HeliumIcon,
  IconLink,
  ImageLink,
  ReleaseInfo,
  Teaser,
  TextLink
}
import sbtghactions.GenerativePlugin, GenerativePlugin.autoImport._

object TypelevelMicrositePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlHeliumConfig = settingKey[Helium]("The Helium configuration")
  }

  import autoImport._

  override def requires = MdocPlugin && LaikaPlugin && GenerativePlugin

  override def projectSettings = Seq(
    Laika / sourceDirectories := Seq(mdocOut.value),
    laikaTheme := tlHeliumConfig.value.build,
    tlHeliumConfig := {
      Helium
        .defaults
        .site
        .layout(
          contentWidth = px(860),
          navigationWidth = px(275),
          topBarHeight = px(35),
          defaultBlockSpacing = px(10),
          defaultLineHeight = 1.5,
          anchorPlacement = laika.helium.config.AnchorPlacement.Right
        )
        .site
        .topNavigationBar(
          homeLink = ImageLink.external(
            "https://typelevel.org",
            Image.external("https://typelevel.org/img/logo.svg")
          ),
          navLinks = Seq(
            IconLink.external(
              "https://www.javadoc.io/doc/org.http4s/http4s-dom_sjs1_2.13/latest/org/http4s/dom/index.html",
              HeliumIcon.api,
              options = Styles("svg-link")),
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
    ThisBuild / githubWorkflowAddedJobs +=
      WorkflowJob(
        "publish-site",
        "Publish Site",
        scalas = List(crossScalaVersions.value.head),
        javas = githubWorkflowJavaVersions.value.toList,
        cond = Some("github.event_name != 'pull_request'"),
        needs = List("build"),
        steps = githubWorkflowJobSetup.value.toList ::: List(
          WorkflowStep.Sbt(List("mdoc", "laikaSite"), name = Some("Generate site")),
          WorkflowStep.Use(
            UseRef.Public("peaceiris", "actions-gh-pages", "v3"),
            Map(
              "github_token" -> s"$${{ secrets.GITHUB_TOKEN }}",
              "publish_dir" -> s"${(ThisBuild / baseDirectory).value.toPath.toAbsolutePath.relativize(((Laika / target).value / "site").toPath)}",
              "publish_branch" -> "gh-pages"
            ),
            name = Some("Publish")
          )
        )
      )
  )

}
