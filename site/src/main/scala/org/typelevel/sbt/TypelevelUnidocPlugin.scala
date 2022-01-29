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
import sbtunidoc.ScalaUnidocPlugin
import laika.sbt.LaikaPlugin
import org.typelevel.sbt.kernel.V
import org.typelevel.sbt.gha.GitHubActionsKeys._

object TypelevelUnidocPlugin extends AutoPlugin {

  override def requires = TypelevelSitePlugin && ScalaUnidocPlugin

  override def trigger = allRequirements

  import TypelevelSitePlugin.autoImport._
  import LaikaPlugin.autoImport._
  import ScalaUnidocPlugin.autoImport._

  override def buildSettings = Seq(
    tlSiteApiUri := Some(uri("api/")),
    apiURL := {
      tlSiteApiUri.value.flatMap { api =>
        if (api.isAbsolute)
          Some(api.toURL)
        else // resolve the api relative to the homepage
          homepage.value.map(_.toURI.resolve(api).toURL)
      }
    }
  )

  override def projectSettings = Seq(
    laikaIncludeAPI := {
      // include the API docs, only if this is not a snapshot nor a pre-release
      // so long as tlSiteKeepFiles is true, it won't delete existing API docs
      val isRelease = !isSnapshot.value && V.unapply(version.value).exists(!_.isPrerelease)

      // If we're not in CI, we'll include them anyway
      !githubIsWorkflowBuild.value || isRelease
    },
    laikaGenerateAPI / mappings := (ScalaUnidoc / packageDoc / mappings).value
  )

}
