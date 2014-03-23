package org.typelevel.sbt

import sbt._
import sbt.Keys._

import sbtrelease.ReleasePlugin._

import Releasing.Stages

object Typelevel extends Plugin {

  type Sett = Def.Setting[_]

  def versioningSettings: Seq[Sett] = List(
    version := Version(TypelevelKeys.currentSeries.value, TypelevelKeys.currentVersion.value).id
  )

  def releaseSettings: Seq[Sett] = sbtrelease.ReleasePlugin.releaseSettings ++ List(
    ReleaseKeys.releaseProcess :=
      Stages.checks ++
      Stages.versions ++
      Stages.pre ++
      Stages.publish ++
      Stages.post
  )

  def typelevelSettings: Seq[Sett] =
    versioningSettings ++
    releaseSettings

}
