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

import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._

import Keys._
import TypelevelKernelPlugin.autoImport._

object TypelevelSonatypePlugin extends AutoPlugin {

  override def requires = MimaPlugin

  override def trigger = allRequirements

  object autoImport {}

  override def globalSettings = Seq(
    tlCommandAliases += {
      "tlRelease" -> List(
        "reload",
        "project /",
        "+mimaReportBinaryIssues",
        "+publish",
        "sonaRelease")
    }
  )

  override def buildSettings =
    Seq(
      autoAPIMappings := true
    )

  override def projectSettings = Seq(
    publishMavenStyle := true, // we want to do this unconditionally, even if publishing a plugin
    publishTo := {
      val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
      if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
      else localStaging.value
    },
    apiURL := apiURL.value.orElse(hostedApiUrl.value)
  )

  private[sbt] lazy val hostedApiUrl =
    Def.setting(javadocioUrl.value.orElse(sonatypeApiUrl.value))

  private lazy val javadocioUrl = Def.setting {
    if (isSnapshot.value || !publishArtifact.value)
      None // javadoc.io doesn't support snapshots, or unpublished modules ;)
    else
      CrossVersion(
        crossVersion.value,
        scalaVersion.value,
        scalaBinaryVersion.value
      ).map { cross =>
        url(
          s"https://www.javadoc.io/doc/${organization.value}/${cross(moduleName.value)}/${version.value}/")
      }
  }

  private lazy val sonatypeApiUrl = Def.setting {
    if (publishArtifact.value)
      CrossVersion(
        crossVersion.value,
        scalaVersion.value,
        scalaBinaryVersion.value
      ).map { cross =>
        val host = "central.sonatype.com"
        val repo = if (isSnapshot.value) "snapshots" else "releases"
        val org = organization.value.replace('.', '/')
        val mod = cross(moduleName.value)
        val ver = version.value
        url(
          s"https://$host/service/local/repositories/$repo/archive/$org/$mod/$ver/$mod-$ver-javadoc.jar/!/index.html")
      }
    else None
  }
}
