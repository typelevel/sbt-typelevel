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

import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.git
import org.typelevel.sbt.kernel.GitHelper
import org.typelevel.sbt.kernel.V
import sbt._

import scala.util.Try
import Keys._

object TypelevelVersioningPlugin extends AutoPlugin {

  override def requires = GitPlugin
  override def trigger = allRequirements

  object autoImport {
    lazy val tlBaseVersion =
      settingKey[String]("The base version for the series your project is in. e.g., 0.2, 3.5")
    lazy val tlUntaggedAreSnapshots =
      settingKey[Boolean](
        "If true, an untagged commit is given a snapshot version, e.g. 0.4-00218f9-SNAPSHOT. If false, it is given a release version, e.g. 0.4-00218f9. (default: true)")

    lazy val currentRelease = Def.setting {
      // some tricky logic here ...
      // if the latest release is a pre-release (e.g., M or RC)
      // and there are no stable releases it is bincompatible with,
      // then for all effective purposes it is the current release

      val release = previousReleases.value match {
        case head :: tail if head.isPrerelease =>
          tail
            .filterNot(_.isPrerelease)
            .find(head.copy(prerelease = None).mustBeBinCompatWith(_))
            .orElse(Some(head))
        case releases => releases.headOption
      }

      release.map(_.toString)
    }

    // latest tagged release, including pre-releases
    lazy val currentPreRelease = Def.setting {
      previousReleases.value.headOption.map(_.toString)
    }

    lazy val previousReleases = Def.setting {
      val currentVersion = V(version.value).map(_.copy(prerelease = None))
      GitHelper.previousReleases(fromHead = true, strict = false).filter { v =>
        currentVersion.forall(v.copy(prerelease = None) <= _)
      }
    }

  }

  import autoImport._

  override def buildSettings: Seq[Setting[_]] = Seq(
    versionScheme := Some("early-semver"),
    tlUntaggedAreSnapshots := true,
    isSnapshot := {
      val isUntagged = taggedVersion.value.isEmpty
      val dirty = git.gitUncommittedChanges.value
      dirty || (isUntagged && tlUntaggedAreSnapshots.value)
    },
    git.gitCurrentTags := {
      // https://docs.github.com/en/actions/learn-github-actions/environment-variables
      // GITHUB_REF_TYPE is either `branch` or `tag`
      if (sys.env.get("GITHUB_REF_TYPE").contains("branch"))
        // we are running in a workflow job that was *not* triggered by a tag
        // so, we discard tags that would affect our versioning
        git.gitCurrentTags.value.flatMap {
          case V.Tag(_) => None
          case other => Some(other)
        }
      else
        git.gitCurrentTags.value
    },
    version := {
      import scala.sys.process._

      val baseV = V(tlBaseVersion.value)
        .filter(v => v.patch.isEmpty && v.prerelease.isEmpty)
        .getOrElse(sys.error(s"tlBaseVersion must be of form x.y: ${tlBaseVersion.value}"))

      val taggedV =
        if (git.gitUncommittedChanges.value)
          None // tree is dirty, so ignore the tags
        else taggedVersion.value.map(_.toString)

      var version = taggedV.getOrElse {
        // No tag, so we build our version based on this commit

        val latestInSeries = GitHelper
          .previousReleases(true)
          .filterNot(_.isPrerelease) // TODO Ordering of pre-releases is arbitrary
          .headOption
          .flatMap {
            case previous if previous > baseV =>
              sys.error(s"Your tlBaseVersion $baseV is behind the latest tag $previous")
            case previous if baseV.isSameSeries(previous) =>
              Some(previous)
            case _ => None
          }

        // version here is the prefix used further to build a final version number
        var version = latestInSeries.fold(tlBaseVersion.value)(_.toString)

        // Looks for the distance to latest release in this series
        latestInSeries.foreach { latestInSeries =>
          Try(s"git describe --tags --match v$latestInSeries".!!.trim)
            .collect { case Description(distance) => distance }
            .foreach { distance => version += s"-$distance" }
        }

        git.gitHeadCommit.value.foreach { sha => version += s"-${sha.take(7)}" }
        version
      }

      V(version) match {
        case None =>
          sys.error(s"version must be semver format: $version")
        case Some(v) if !(v.isSameSeries(baseV) || v >= baseV) =>
          sys.error(s"Your current version $version cannot be less than tlBaseVersion $baseV")
        case _ => // do nothing
      }

      // Even if version was provided by a tag, we check for uncommited changes
      if (git.gitUncommittedChanges.value) {
        import java.time.Instant
        // Drop the sub-second precision
        val now = Instant.ofEpochSecond(Instant.now().getEpochSecond())
        val formatted = now.toString.replace("-", "").replace(":", "")
        version += s"-$formatted"
      }

      if (isSnapshot.value) version += "-SNAPSHOT"

      version
    }
  )

  private val Description = """^.*-(\d+)-[a-zA-Z0-9]+$""".r

  private def taggedVersion = Def.setting {
    git.gitCurrentTags.value.collect { case V.Tag(v) => v }.sorted.lastOption
  }

}
