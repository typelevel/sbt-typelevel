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
import org.typelevel.sbt.kernel.GitHelper
import org.typelevel.sbt.kernel.V
import sbt._

import Keys._
import MimaPlugin.autoImport._

object TypelevelMimaPlugin extends AutoPlugin {

  override def requires = MimaPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val tlVersionIntroduced =
      settingKey[Map[String, String]](
        "A map scalaBinaryVersion -> version e.g. Map('2.13' -> '1.5.2', '3' -> '1.7.1') used to indicate that a particular crossScalaVersions value was introduced in a given version (default: empty).")
    lazy val tlMimaPreviousVersions = settingKey[Set[String]](
      "A set of previous versions to compare binary-compatibility against (default: auto-populated from git tags and the tlVersionIntroduced setting)")
  }

  import autoImport._
  import TypelevelKernelPlugin.autoImport._
  import TypelevelKernelPlugin.skipIfIrrelevant

  override def buildSettings = Seq[Setting[_]](
    tlVersionIntroduced := Map.empty,
    tlMimaPreviousVersions := {
      require(
        versionScheme.value.contains("early-semver"),
        "Only early-semver versioning scheme supported.")

      val current = V(version.value)
        // Consider it as a real release, for purposes of compat-checking
        .map(_.copy(prerelease = None))
        .getOrElse(sys.error(s"Version must be semver format: ${version.value}"))

      val previous = GitHelper
        .previousReleases()
        .filterNot(_.isPrerelease)
        .filter(current.mustBeBinCompatWith(_))

      previous.map(_.toString).toSet
    }
  )

  override def projectSettings = Seq[Setting[_]](
    mimaReportBinaryIssues := {
      if (tlSkipIrrelevantScalas.value && (mimaReportBinaryIssues / skip).value)
        ()
      else mimaReportBinaryIssues.value
    },
    skipIfIrrelevant(mimaReportBinaryIssues),
    tlMimaPreviousVersions := {
      val introduced = tlVersionIntroduced
        .value
        .get(scalaBinaryVersion.value)
        .map(v => V(v).getOrElse(sys.error(s"Version must be semver format: $v")))

      tlMimaPreviousVersions
        .value
        .map(v => V(v).getOrElse(sys.error(s"Version must be semver format: $v")))
        .filter(v => introduced.forall(v >= _))
        .map(_.toString)
    },
    mimaPreviousArtifacts := {
      if (publishArtifact.value)
        tlMimaPreviousVersions.value.map { v =>
          projectID.value.withRevision(v).withExplicitArtifacts(Vector.empty)
        }
      else
        Set.empty
    }
  )

}
