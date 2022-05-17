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

import cats.effect.{Resource, Sync}
import laika.ast.Path
import laika.config.Config
import laika.io.model.InputTree
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.parse.code.languages.DottySyntax
import laika.rewrite.DefaultTemplatePath
import laika.theme.{Theme, ThemeBuilder, ThemeProvider}

import java.net.URL

object TypelevelHeliumExtensions {

  @deprecated("Use overload with scala3 parameter", "0.4.7")
  def apply(license: Option[(String, URL)], related: Seq[(String, URL)]): ThemeProvider =
    apply(license, related, false)

  def apply(
      license: Option[(String, URL)],
      related: Seq[(String, URL)],
      scala3: Boolean
  ): ThemeProvider = new ThemeProvider {
    def build[F[_]](implicit F: Sync[F]): Resource[F, Theme[F]] =
      ThemeBuilder[F]("Typelevel Helium Extensions")
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

}
