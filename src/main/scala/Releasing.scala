package org.typelevel.sbt

import sbt._
import sbt.Keys._

import com.typesafe.sbt.pgp.PgpKeys._

import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._

object Releasing {

  object Stages {
    val checks: Seq[ReleaseStep] = List(
      checkSnapshotDependencies,
      runTest
    )

    val versions: Seq[ReleaseStep] = List(
      inquireVersions
    )

    val pre: Seq[ReleaseStep] = List(
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease
    )

    val publish: Seq[ReleaseStep] = List(
      publishArtifacts
    )

    val post: Seq[ReleaseStep] = List(
      setNextVersion,
      setMimaVersion,
      commitNextVersion
    )
  }

  lazy val versions = AttributeKey[Versions]("typelevel-release-versions")

  private def readVersion(prompt: String): Version.Relative =
    SimpleReader.readLine(prompt) match {
      case Some(input) => Version.Relative.fromString(input).getOrElse(sys.error("version format error"))
      case None => sys.error("no version provided")
    }

  val inquireVersions: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)

    st.log.info(s"Current version is: ${extracted.get(version)}")

    val releaseV = readVersion("Release version: ")
    val nextV = readVersion("Next version: ")

    st.put(versions, (releaseV, nextV))
  }

  private def writeVersion(st: State, version: Version.Relative): Unit = {
    val extracted = Project.extract(st)

    val file = extracted.get(versionFile)
    val series = extracted.get(TypelevelKeys.currentSeries)

    val contents = s"""|
    |import org.typelevel.sbt._
    |import org.typelevel.sbt.ReleaseSeries._
    |import org.typelevel.sbt.Version._
    |
    |TypelevelKeys.currentSeries in ThisBuild := $series
    |
    |TypelevelKeys.currentVersion in ThisBuild := $version
    |""".stripMargin

    IO.write(file, contents, append = false)
  }

  private def setVersions(select: Versions => Version.Relative): ReleaseStep = { st: State =>
    val version = select(st.get(versions).getOrElse(sys.error("versions must be set")))
    val series = Project.extract(st).get(TypelevelKeys.currentSeries)

    st.log.info(s"Setting version to ${series.id}.${version.id}")

    writeVersion(st, version)

    reapply(Seq(TypelevelKeys.currentVersion in ThisBuild := version), st)
  }

  val setReleaseVersion: ReleaseStep = setVersions(_._1)
  val setNextVersion: ReleaseStep = setVersions(_._2)

  val setMimaVersion: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)

    val file = extracted.get(versionFile)
    val (version, _) = st.get(versions).getOrElse(sys.error("versions must be set"))

    val contents = s"""|
    |TypelevelKeys.lastRelease in ThisBuild := $version
    |""".stripMargin

    IO.write(file, contents, append = true)

    reapply(Seq(TypelevelKeys.lastRelease in ThisBuild := version), st)
  }


  val publishArtifacts: ReleaseStep = ReleaseStep(
    action = st => {
      val extracted = Project.extract(st)
      val ref = extracted.get(thisProjectRef)
      val task = if (extracted.get(TypelevelKeys.signArtifacts)) publishSigned else publish
      extracted.runAggregated(task in ref, st)
    },
    check = st => {
      // getPublishTo fails if no publish repository is set up.
      val ex = Project.extract(st)
      val ref = ex.get(thisProjectRef)
      Classpaths.getPublishTo(ex.get(publishTo in Global in ref))
      st
    },
    enableCrossBuild = true
  )

}
