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

import cats.effect.Sync
import laika.bundle.ExtensionBundle
import laika.factory.Format
import laika.io.model.InputTree
import laika.theme.{Theme, ThemeProvider}

final class LaikaThemeProviderOps private[sbt] (provider: ThemeProvider) {

  def extend(extensions: ThemeProvider): ThemeProvider = new ThemeProvider {
    def build[F[_]: Sync] = for {
      base <- provider.build
      ext <- extensions.build
    } yield {
      def overrideInputs(base: InputTree[F], overrides: InputTree[F]): InputTree[F] = {
        val overridePaths = overrides.allPaths.toSet
        val filteredBaseInputs = InputTree(
          textInputs = base.textInputs.filterNot(in => overridePaths.contains(in.path)),
          binaryInputs = base.binaryInputs.filterNot(in => overridePaths.contains(in.path)),
          parsedResults = base.parsedResults.filterNot(in => overridePaths.contains(in.path)),
          sourcePaths = base.sourcePaths
        )
        overrides ++ filteredBaseInputs
      }

      new Theme[F] {
        override def inputs: InputTree[F] = overrideInputs(base.inputs, ext.inputs)
        override def extensions: Seq[ExtensionBundle] = base.extensions ++ ext.extensions
        override def treeProcessor: Format => Theme.TreeProcessor[F] = fmt =>
          base.treeProcessor(fmt).andThen(ext.treeProcessor(fmt))
      }
    }
  }

}
