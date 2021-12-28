package org.typelevel.sbt

import sbt._, Keys._
import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.git
import org.typelevel.sbt.kernel.V

import scala.util.Try
import org.typelevel.sbt.kernel.GitHelper

object TypelevelVersioningPlugin extends AutoPlugin {

  override def requires = GitPlugin
  override def trigger = allRequirements

  object autoImport {
    lazy val tlBaseVersion =
      settingKey[String]("The base version for the series your project is in. e.g., 0.2, 3.5")
    lazy val tlHashSnapshots =
      settingKey[Boolean]("If true, a hash version implies this is a snapshot (default: true).")
  }

  import autoImport._

  override def buildSettings: Seq[Setting[_]] = Seq(
    versionScheme := Some("early-semver"),
    tlHashSnapshots := true,
    isSnapshot := {
      val isVersionTagged = getTaggedVersion(git.gitCurrentTags.value).isDefined
      val dirty = git.gitUncommittedChanges.value
      !isVersionTagged && (tlHashSnapshots.value || dirty)
    },
    version := {
      import scala.sys.process._

      val taggedVersion = getTaggedVersion(git.gitCurrentTags.value)
      taggedVersion.getOrElse {
        val baseV = V(tlBaseVersion.value)
          .getOrElse(sys.error(s"tlBaseVersion must be semver format: ${tlBaseVersion.value}"))

        val latestInSeries = GitHelper.previousReleases().headOption.flatMap { previous =>
          if (previous > baseV)
            sys.error(s"Your tlBaseVersion $baseV is behind the latest tag $previous")
          else if (baseV.isSameSeries(previous))
            Some(previous)
          else
            None
        }

        var version = latestInSeries.fold(tlBaseVersion.value)(_.toString)

        // Looks for the distance to latest release in this series
        latestInSeries.foreach { latestInSeries =>
          Try(s"git describe --tags --match v$latestInSeries".!!.trim)
            .collect { case Description(distance) => distance }
            .foreach { distance => version += s"-$distance" }
        }

        git.gitHeadCommit.value.foreach { sha => version += s"-${sha.take(7)}" }
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
    }
  )

  val Description = """^.*-(\d+)-[a-zA-Z0-9]+$""".r

  def getTaggedVersion(tags: Seq[String]): Option[String] =
    tags.collect { case v @ V.Tag(_) => v }.headOption

}
