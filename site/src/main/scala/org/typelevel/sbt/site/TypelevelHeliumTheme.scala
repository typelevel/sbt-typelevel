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

import cats.effect.Resource
import cats.effect.Sync
import laika.ast.LengthUnit._
import laika.ast.Path
import laika.ast._
import laika.config.Config
import laika.helium.Helium
import laika.helium.config.Favicon
import laika.helium.config.HeliumIcon
import laika.helium.config.IconLink
import laika.helium.config.ImageLink
import laika.io.model.InputTree
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.rewrite.DefaultTemplatePath
import laika.theme.Theme
import laika.theme.ThemeBuilder
import laika.theme.ThemeProvider
import sbt.librarymanagement.Developer

import java.net.URL

object TypelevelHeliumTheme {

  def apply(
      repo: Option[String],
      developers: Seq[Developer],
      version: String,
      apiUrl: Option[URL],
      browseUrl: Option[URL],
      license: Option[(String, URL)],
      related: Seq[(String, URL)]
  ): ThemeProvider = new ThemeProvider {

    def build[F[_]](implicit F: Sync[F]): Resource[F, Theme[F]] =
      new LaikaThemeProviderOps(base).extend(extensions).build

    def base = Helium
      .defaults
      .site
      .metadata(
        title = repo,
        authors = developers.map(_.name),
        language = Some("en"),
        version = Some(version)
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
        navLinks = apiUrl.toList.map { url =>
          IconLink.external(
            url.toString,
            HeliumIcon.api,
            options = Styles("svg-link")
          )
        } ++ List(
          IconLink.external(
            browseUrl.fold("https://github.com/typelevel")(_.toString),
            HeliumIcon.github,
            options = Styles("svg-link")),
          IconLink.external("https://discord.gg/XF3CXcMzqD", HeliumIcon.chat),
          IconLink.external("https://twitter.com/typelevel", HeliumIcon.twitter)
        )
      )
      .build

    def extensions = new ThemeProvider {
      def build[F[_]](implicit F: Sync[F]) = ThemeBuilder[F]("Typelevel Helium Extensions")
        .addInputs(
          InputTree[F]
            .addStream(
              F.blocking(getClass.getResourceAsStream("helium/default.template.html")),
              DefaultTemplatePath.forHTML
            )
            .addStream(
              F.blocking(getClass.getResourceAsStream("helium/site/styles.css")),
              Path.Root / "site" / "styles.css"
            )
        )
        .addExtensions(GitHubFlavor, SyntaxHighlighting)
        .addBaseConfig(licenseConfig.withFallback(relatedConfig))
        .build
    }

    def licenseConfig =
      license.fold(Config.empty) {
        case (name, url) =>
          Config
            .empty
            .withValue("typelevel.site.license.name", name)
            .withValue("typelevel.site.license.url", url.toString)
            .build
      }

    def relatedConfig =
      Config
        .empty
        .withValue(
          "typelevel.site.related",
          related.map {
            case (name, url) =>
              Map("name" -> name, "url" -> url.toString)
          })
        .build

  }

}
