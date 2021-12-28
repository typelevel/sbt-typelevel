package org.typelevel.sbt

import sbt._, Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import xerial.sbt.Sonatype, Sonatype.autoImport._

object TypelevelSonatypePlugin extends AutoPlugin {

  override def requires = MimaPlugin && Sonatype

  override def trigger = allRequirements

  object autoImport {
    lazy val tlSonatypeUseLegacyHost =
      settingKey[Boolean]("Publish to oss.sonatype.org instead of s01")
  }

  import autoImport._

  override def buildSettings =
    Seq(tlSonatypeUseLegacyHost := false) ++
      addCommandAlias(
        "release",
        "; reload; project /; +mimaReportBinaryIssues; +publish; sonatypeBundleReleaseIfRelevant")

  override def projectSettings = Seq(
    publishMavenStyle := true, // we want to do this unconditionally, even if publishing a plugin
    sonatypeProfileName := organization.value,
    publishTo := sonatypePublishToBundle.value,
    commands += sonatypeBundleReleaseIfRelevant,
    sonatypeCredentialHost := {
      if (tlSonatypeUseLegacyHost.value)
        "oss.sonatype.org"
      else
        "s01.oss.sonatype.org"
    }
  )

  private def sonatypeBundleReleaseIfRelevant: Command =
    Command.command("sonatypeBundleReleaseIfRelevant") { state =>
      if (state.getSetting(isSnapshot).getOrElse(false))
        state // a snapshot is good-to-go
      else // a non-snapshot releases as a bundle
        Command.process("sonatypeBundleRelease", state)
    }
}
