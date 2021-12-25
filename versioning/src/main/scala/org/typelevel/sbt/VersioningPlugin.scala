package org.typelevel.sbt

import sbt._, Keys._
import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.git

object VersioningPlugin extends AutoPlugin {

  override def requires = GitPlugin
  override def trigger = allRequirements

  object autoImport {
    lazy val tlBaseVersion =
      settingKey[String]("The base version for the series your project is in. e.g., 0.2, 3.5")
    lazy val tlHashSnapshots =
      settingKey[Boolean]("If true, a hash version implies this is a snapshot.")
  }

  import autoImport._

  val ReleaseTag = """^v((?:\d+\.){2}\d+(?:-.*)?)$""".r

  override def buildSettings: Seq[Setting[_]] = Seq(
    tlHashSnapshots := true,
    isSnapshot := {
      val isVersionTagged = findVersionTag(git.gitCurrentTags.value).isDefined
      val dirty = git.gitUncommittedChanges.value
      !isVersionTagged && (tlHashSnapshots.value || dirty)
    },
    version := {
      val taggedVersion = git.gitCurrentTags.value.find(ReleaseTag.unapplySeq(_).isDefined)
      taggedVersion.getOrElse {
        var version = tlBaseVersion.value
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

  def findVersionTag(tags: Seq[String]): Option[String] =
    tags.find(ReleaseTag.unapplySeq(_).isDefined)

}
