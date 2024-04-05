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

import java.net.URL

object GenericSiteSettings {

  val apiLink: Initialize[Option[IconLink]] = setting {
    tlSiteApiUrl.value.map { url => IconLink.external(url.toString, HeliumIcon.api) }
  }

  val githubLink: Initialize[Option[IconLink]] = setting {
    scmInfo.value.map { info => IconLink.external(info.browseUrl.toString, HeliumIcon.github) }
  }

  val themeExtensions: Initialize[ThemeProvider] = setting {
    new ThemeProvider {
      def build[F[_]](implicit F: Async[F]): Resource[F, Theme[F]] =
        ThemeBuilder[F]("sbt-typelevel-site Helium Extensions")
          .addInputs(
            tlSiteApiUrl.value.fold(InputTree[F]) { url =>
              InputTree[F].addString(htmlForwarder(url), Path.Root / "api" / "index.html")
            }
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

  private def htmlForwarder(to: URL) =
    s"""|<!DOCTYPE html>
        |<meta charset="utf-8">
        |<meta http-equiv="refresh" content="0; URL=$to">
        |<link rel="canonical" href="$to">
        |""".stripMargin

}
