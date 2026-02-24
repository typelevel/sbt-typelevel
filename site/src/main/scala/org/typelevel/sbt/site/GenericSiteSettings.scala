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

package org.typelevel.sbt.site

import cats.effect.Async
import cats.effect.Resource
import laika.ast.Path
import laika.config.SyntaxHighlighting
import laika.format.Markdown.GitHubFlavor
import laika.helium.Helium
import laika.helium.config.HeliumIcon
import laika.helium.config.IconLink
import laika.io.model.InputTree
import laika.parse.code.languages.ScalaSyntax
import laika.theme.Theme
import laika.theme.ThemeBuilder
import laika.theme.ThemeProvider
import org.typelevel.sbt.TypelevelGitHubPlugin.gitHubUserRepo
import org.typelevel.sbt.TypelevelKernelPlugin.autoImport.tlIsScala3
import org.typelevel.sbt.TypelevelSitePlugin.autoImport.tlSiteApiUrl
import sbt.Def._
import sbt.Keys.developers
import sbt.Keys.scmInfo
import sbt.Keys.version
import sbt.Keys.baseDirectory
import sbt._

import java.net.URL

object GenericSiteSettings {

  val apiLink: Initialize[Option[IconLink]] = setting {
    tlSiteApiUrl.value.map { url => IconLink.external(url.toString, HeliumIcon.api) }
  }

  val githubLink: Initialize[Option[IconLink]] = setting {
    scmInfo.value.map { info => IconLink.external(info.browseUrl.toString, HeliumIcon.github) }
  }

  val themeExtensions: Initialize[ThemeProvider] = setting {

    val rootDir = (ThisBuild / baseDirectory).value
    val docsDir = rootDir / "docs"
    val userProvided404 = (docsDir / "404.md").exists() || (docsDir / "404.html").exists()

    new ThemeProvider {
      def build[F[_]](implicit F: Async[F]): Resource[F, Theme[F]] =
        ThemeBuilder[F]("sbt-typelevel-site Helium Extensions")
          .addInputs(
            tlSiteApiUrl.value
              .fold(InputTree[F]) { url =>
                InputTree[F].addString(htmlForwarder(url), Path.Root / "api" / "index.html")
              }
              .merge(
                if (userProvided404)
                  InputTree[F]
                else
                  InputTree[F].addString(default404Html, Path.Root / "404.html")
              )
          )
          .addExtensions(
            GitHubFlavor,
            if (tlIsScala3.value)
              SyntaxHighlighting.withSyntaxBinding("scala", ScalaSyntax.Scala3)
            else SyntaxHighlighting
          )
          .build
    }
  }

  val defaults: Initialize[Helium] = setting {
    Helium
      .defaults
      .extendWith(themeExtensions.value)
      .site
      .metadata(
        title = gitHubUserRepo.value.map(_._2),
        authors = developers.value.map(_.name),
        language = Some("en"),
        version = Some(version.value)
      )
      .site
      .topNavigationBar(
        navLinks = apiLink.value.toList ++ githubLink.value.toList
      )
  }

private val default404Html: String =
  """|<!DOCTYPE html>
     |<html lang="en">
     |<head>
     |  <meta charset="utf-8">
     |  <meta name="viewport" content="width=device-width, initial-scale=1">
     |  <title>Page not found</title>
     |  <link rel="stylesheet" href="helium/site/laika-helium.css">
     |  <script src="helium/site/laika-helium.js"></script>
     |</head>
     |<body>
     |  <main class="content">
     |    <h1 style="text-align:center; font-size:9rem; color:#d2d6dc; margin-top:10rem;">404</h1>
     |    <h2 style="text-align:center; margin-top:4rem;">Page Not Found</h2>
     |    <p style="text-align:center; margin-top:1.5rem;">
     |      Sorry, the page you were looking for does not exist
     |    </p>
     |    <p style="text-align:center; margin-top:1.5rem;">
     |      <a href="index.html">Click here to go back to the home page</a>
     |    </p>
     |  </main>
     |</body>
     |</html>
     |""".stripMargin

  private def htmlForwarder(to: URL) =
    s"""|<!DOCTYPE html>
        |<meta charset="utf-8">
        |<meta http-equiv="refresh" content="0; URL=$to">
        |<link rel="canonical" href="$to">
        |""".stripMargin

}
