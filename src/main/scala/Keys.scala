package org.typelevel.sbt

import sbt._

object TypelevelKeys {

  lazy val currentSeries = SettingKey[ReleaseSeries]("current-series", "The current release series of this branch")

  // used to compute `version`
  lazy val currentVersion = SettingKey[Version.Relative]("current-version", "The current version of this branch")

  // can be left unset
  lazy val lastRelease = SettingKey[Version.Relative]("last-release", "The last release in the series of this branch")

}
