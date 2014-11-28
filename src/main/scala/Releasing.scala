package org.typelevel.sbt

import sbt._
import sbt.Keys._

import com.typesafe.sbt.pgp.PgpKeys._

import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._

import org.typelevel.sbt.TypelevelPlugin.TypelevelKeys

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

  private def readSeries(prompt: String): Option[ReleaseSeries] =
    SimpleReader.readLine(prompt) match {
      case Some(input) =>
        Some(ReleaseSeries.fromString(input).getOrElse(sys.error("version format error")))
      case None =>
        None
    }

  private def readVersion(prompt: String): Version.Relative =
    SimpleReader.readLine(prompt) match {
      case Some(input) =>
        Version.Relative.fromString(input).getOrElse(sys.error("version format error"))
      case None =>
        sys.error("no version provided")
    }

  val inquireVersions: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)
    val releaseS = extracted.get(TypelevelKeys.series)

    st.log.info(s"Current version is: ${extracted.get(version)}")

    val releaseV = readVersion("Release (relative) version: ")

    val (nextS, nextV) = readSeries(s"Next release series [${releaseS.id}]: ") match {
      case None =>
        st.log.info("Not bumping release series")
        (releaseS, readVersion("Next (relative) version: "))
      case Some(series) =>
        st.log.info(s"Bumping release series to ${series.id}, setting next relative version to 0-SNAPSHOT")
        (series, Version.Relative(0, Version.Snapshot))
    }

    st.put(versions, (Version(releaseS, releaseV), Version(nextS, nextV)))
  }

  private def writeVersion(st: State, version: Version): Unit = {
    val extracted = Project.extract(st)

    val file = extracted.get(versionFile)

    val contents = s"""|import org.typelevel.sbt.ReleaseSeries
                       |import org.typelevel.sbt.Version._
                       |
                       |TypelevelKeys.series in ThisBuild := ${version.series}
                       |
                       |TypelevelKeys.relativeVersion in ThisBuild := ${version.relative}
                       |""".stripMargin

    IO.write(file, contents, append = false)
  }

  private def setVersions(select: Versions => Version): ReleaseStep = { st: State =>
    val version = select(st.get(versions).getOrElse(sys.error("versions must be set")))

    st.log.info(s"Setting version to ${version.id}")

    writeVersion(st, version)

    reapply(Seq(
      TypelevelKeys.series in ThisBuild := version.series,
      TypelevelKeys.relativeVersion in ThisBuild := version.relative
    ), st)
  }

  val setReleaseVersion: ReleaseStep = setVersions(_._1)
  val setNextVersion: ReleaseStep = setVersions(_._2)

  val setMimaVersion: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)

    val (releaseV, nextV) = st.get(versions).getOrElse(sys.error("versions must be set"))

    extracted.get(TypelevelKeys.stability) match {
      case Stability.Stable if releaseV.series == nextV.series =>
        val file = extracted.get(versionFile)

        val contents = s"""|
        |TypelevelKeys.lastRelease in ThisBuild := ${releaseV.relative}
        |""".stripMargin

        IO.write(file, contents, append = true)

        reapply(Seq(TypelevelKeys.lastRelease in ThisBuild := releaseV.relative), st)

      case Stability.Stable =>
        st.log.info("Bumped release series; not setting `lastRelease`")
        st

      case Stability.Development =>
        st.log.info("Unstable branch; not setting `lastRelease`")
        st
    }
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
