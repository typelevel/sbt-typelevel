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
import gha.GenerativePlugin, GenerativePlugin.autoImport._
import scala.io.Source
import java.util.Base64

object TypelevelMicrositePlugin extends AutoPlugin {

  object autoImport {
    lazy val tlHeliumConfig = settingKey[Helium]("The Helium configuration")
    lazy val tlApiUrl = settingKey[URL]("Url to the API scaladocs")
  }

  import autoImport._

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

  private def getSvgLogo: String = {
    val src = Source.fromURL(getClass.getResource("/logo.svg"))
    try {
      Base64.getEncoder().encodeToString(src.mkString.getBytes)
    } finally {
      src.close()
    }
  }

}
