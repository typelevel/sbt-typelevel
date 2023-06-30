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

import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GenerativePlugin.autoImport._
import sbt.Keys._
import sbt._

object TypelevelSiteCiReleasePlugin extends AutoPlugin {

  override def requires = TypelevelCiSigningPlugin && TypelevelSitePlugin && TypelevelSonatypeCiReleasePlugin

  override def trigger = allRequirements

  import TypelevelCiSigningPlugin.autoImport._
  import TypelevelSitePlugin.autoImport._

  override def projectSettings = Seq(
    githubWorkflowArtifactUpload := false,
    ThisBuild / githubWorkflowArtifactDownloadExtraArtifacts += "site",
    ThisBuild / githubWorkflowPublishNeeds += "site",
    tlSitePublish ++= {
      tlCiSigningImportKey.value ++ List(
        WorkflowStep.Sbt(
          List(s"${thisProject.value.id}/${publish.key.toString}"),
          name = Some("Publish site to sonatype"),
          cond = Some(GenerativePlugin.publicationCond.value),
          env = TypelevelSonatypeCiReleasePlugin.env
        )
      ) ++ WorkflowStep.upload(
        List(java.nio.file.Paths.get("target")),
        "site"
      )
    }
  )

}
