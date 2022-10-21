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
import cats.effect.kernel.Resource
import laika.ast.Path
import laika.io.model.InputTree
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.parse.code.languages.DottySyntax
import laika.theme.Theme
import laika.theme.ThemeBuilder
import laika.theme.ThemeProvider

import java.net.URL

@deprecated("Use GenericSiteSettings.extensions", "0.5.0")
object TypelevelHeliumExtensions {

  @deprecated("Use overload with scala3 and apiURL parameter", "0.4.7")
  def apply(license: Option[(String, URL)], related: Seq[(String, URL)]): ThemeProvider =
    apply(license, related, false)

  @deprecated("Use overload with scala3 and apiURL parameter", "0.4.13")
  def apply(
      license: Option[(String, URL)],
      related: Seq[(String, URL)],
      scala3: Boolean): ThemeProvider =
    apply(license, related, false, None)

  @deprecated("Use overload with scala3 and apiURL parameter", "0.5.0")
  def apply(
      license: Option[(String, URL)],
      related: Seq[(String, URL)],
      scala3: Boolean,
      apiUrl: Option[URL]
  ): ThemeProvider = apply(scala3, apiUrl)

  /**
   * @param scala3
   *   whether to use Scala 3 syntax highlighting
   * @param apiUrl
   *   url to API docs
   */
  def apply(
      scala3: Boolean,
      apiUrl: Option[URL]
  ): ThemeProvider = new ThemeProvider {
    def build[F[_]](implicit F: Async[F]): Resource[F, Theme[F]] =
      ThemeBuilder[F]("sbt-typelevel-site Helium Extensions")
        .addInputs(
          apiUrl.fold(InputTree[F]) { url =>
            InputTree[F].addString(htmlForwarder(url), Path.Root / "api" / "index.html")
          }
        )
        .addExtensions(
          GitHubFlavor,
          if (scala3) SyntaxHighlighting.withSyntaxBinding("scala", DottySyntax)
          else SyntaxHighlighting
        )
        .build
  }

  private def htmlForwarder(to: URL) =
    s"""|<!DOCTYPE html>
        |<meta charset="utf-8">
        |<meta http-equiv="refresh" content="0; URL=$to">
        |<link rel="canonical" href="$to">
        |""".stripMargin

}
