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

import cats.effect.Resource
import cats.effect.Sync
import laika.io.model.InputTree
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.rewrite.DefaultTemplatePath
import laika.theme.Theme
import laika.theme.ThemeBuilder
import laika.theme.ThemeProvider

object TypelevelHeliumExtensions extends ThemeProvider {

  override def build[F[_]](implicit F: Sync[F]): Resource[F, Theme[F]] =
    ThemeBuilder[F]("Typelevel Helium Extensions")
      .addInputs(
        InputTree[F].addStream(
          F.blocking(getClass.getResourceAsStream("helium/default.template.html")),
          DefaultTemplatePath.forHTML
        ))
      .addExtensions(GitHubFlavor, SyntaxHighlighting)
      .build

}
