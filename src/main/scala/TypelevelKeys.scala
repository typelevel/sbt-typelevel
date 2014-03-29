package org.typelevel.sbt

import sbt._

object TypelevelKeys {

  lazy val series = SettingKey[ReleaseSeries]("series", "The current release series of this branch")

  lazy val stability = SettingKey[Stability]("stability", "The current stability of this branch")

  // used to compute `version`
  lazy val relativeVersion = SettingKey[Version.Relative]("relativeVersion", "The current version of this branch, relative to the current series")

  // can be left unset
  lazy val lastRelease = SettingKey[Version.Relative]("lastRelease", "The last release in the series of this branch")

  lazy val signArtifacts = SettingKey[Boolean]("signArtifacts", "Sign artifacts before publishing")

}
