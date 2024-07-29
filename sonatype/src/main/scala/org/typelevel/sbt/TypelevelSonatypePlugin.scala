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
import xerial.sbt.Sonatype

import scala.annotation.nowarn

import Keys._
import Sonatype.autoImport._
import TypelevelKernelPlugin.autoImport._

object TypelevelSonatypePlugin extends AutoPlugin {

  override def requires = MimaPlugin && Sonatype

  override def trigger = allRequirements

  object autoImport {
    @deprecated(
      "Use ThisBuild / sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeLegacy",
      "0.7.3"
    )
    lazy val tlSonatypeUseLegacyHost =
      settingKey[Boolean]("Publish to oss.sonatype.org instead of s01 (default: false)")
  }

  import autoImport._

  override def globalSettings = Seq(
    tlCommandAliases += {
      "tlRelease" -> List(
        "reload",
        "project /",
        "+mimaReportBinaryIssues",
        "+publish",
        "tlSonatypeBundleReleaseIfRelevant")
    }
  )

  @nowarn("cat=deprecation")
  override def buildSettings =
    Seq(
      tlSonatypeUseLegacyHost := false,
      autoAPIMappings := true,
      sonatypeCredentialHost := {
        Option(System.getenv("SONATYPE_CREDENTIAL_HOST")).filter(_.nonEmpty).getOrElse {
          if (tlSonatypeUseLegacyHost.value)
            Sonatype.sonatypeLegacy
          else
            Sonatype.sonatype01
        }
      }
    )

  override def projectSettings = Seq(
    publishMavenStyle := true, // we want to do this unconditionally, even if publishing a plugin
    sonatypeProfileName := organization.value,
    publishTo := sonatypePublishToBundle.value,
    commands += sonatypeBundleReleaseIfRelevant,
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
        val host = sonatypeCredentialHost.value
        val repo = if (isSnapshot.value) "snapshots" else "releases"
        val org = organization.value.replace('.', '/')
        val mod = cross(moduleName.value)
        val ver = version.value
        url(
          s"https://$host/service/local/repositories/$repo/archive/$org/$mod/$ver/$mod-$ver-javadoc.jar/!/index.html")
      }
    else None
  }

  private def sonatypeBundleReleaseIfRelevant: Command =
    Command.command("tlSonatypeBundleReleaseIfRelevant") { state =>
      if (state.getSetting(isSnapshot).getOrElse(false))
        state // a snapshot is good-to-go
      else // a non-snapshot releases as a bundle
        Command.process("sonatypeBundleRelease", state)
    }
}
