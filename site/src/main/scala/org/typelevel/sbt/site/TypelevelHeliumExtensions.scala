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

import cats.effect.{Async, Resource}
import laika.ast.Path
import laika.config.Config
import laika.io.model.InputTree
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.parse.code.languages.DottySyntax
import laika.rewrite.DefaultTemplatePath
import laika.theme.Theme
import laika.theme.ThemeBuilder
import laika.theme.ThemeProvider

import java.net.URL

object TypelevelHeliumExtensions {

  @deprecated("Use overload with API url and scala3 parameter", "0.4.7")
  def apply(license: Option[(String, URL)], related: Seq[(String, URL)]): ThemeProvider =
    apply(license, related, false)

  @deprecated("Use overload with API url and scala3 parameter", "0.4.13")
  def apply(
      license: Option[(String, URL)],
      related: Seq[(String, URL)],
      scala3: Boolean): ThemeProvider =
    apply(license, related, false, None)

  /**
   * @param license
   *   name and [[java.net.URL]] of project license
   * @param related
   *   name and [[java.net.URL]] of related projects
   * @param scala3
   *   whether to use Scala 3 syntax highlighting
   * @param apiUrl
   *   url to API docs
   */
  def apply(
      license: Option[(String, URL)],
      related: Seq[(String, URL)],
      scala3: Boolean,
      apiUrl: Option[URL]
  ): ThemeProvider = new ThemeProvider {
    def build[F[_]](implicit F: Async[F]): Resource[F, Theme[F]] =
      ThemeBuilder[F]("Typelevel Helium Extensions")
        .addInputs(
          InputTree[F]
            .addInputStream(
              F.blocking(getClass.getResourceAsStream("helium/default.template.html")),
              DefaultTemplatePath.forHTML
            )
            .addInputStream(
              F.blocking(getClass.getResourceAsStream("helium/site/styles.css")),
              Path.Root / "site" / "styles.css"
            )
            .merge(
              apiUrl.fold(InputTree[F]) { url =>
                InputTree[F].addString(htmlForwarder(url), Path.Root / "api" / "index.html")
              }
            )
        )
        .addExtensions(
          GitHubFlavor,
          if (scala3) SyntaxHighlighting.withSyntaxBinding("scala", DottySyntax)
          else SyntaxHighlighting
        )
        .addBaseConfig(licenseConfig(license).withFallback(relatedConfig(related)))
        .build
  }

  private def licenseConfig(license: Option[(String, URL)]) =
    license.fold(Config.empty) {
      case (name, url) =>
        Config
          .empty
          .withValue("typelevel.site.license.name", name)
          .withValue("typelevel.site.license.url", url.toString)
          .build
    }

  private def relatedConfig(related: Seq[(String, URL)]) =
    Config
      .empty
      .withValue(
        "typelevel.site.related",
        related.map {
          case (name, url) =>
            Map("name" -> name, "url" -> url.toString)
        })
      .build

  private def htmlForwarder(to: URL) =
    s"""|<!DOCTYPE html>
        |<meta charset="utf-8">
        |<meta http-equiv="refresh" content="0; URL=$to">
        |<link rel="canonical" href="$to">
        |""".stripMargin

}
