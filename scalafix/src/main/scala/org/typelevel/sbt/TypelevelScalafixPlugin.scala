package org.typelevel.sbt

import sbt._
import scalafix.sbt.ScalafixPlugin

import Keys._
import ScalafixPlugin.autoImport._

object TypelevelScalafixPlugin extends AutoPlugin {

  override def requires = ScalafixPlugin

  override def trigger = allRequirements

  object autoImport {
    val tlScalafixDependencies = settingKey[Seq[ModuleID]]("The scalafix rule dependencies to enable in the build.")
  }

  import autoImport._

  override def buildSettings = Seq[Setting[_]](
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
    tlScalafixDependencies := Seq(
      "com.github.liancheng" %% "organize-imports" % "0.6.0"
    ),
    scalafixDependencies ++= tlScalafixDependencies.value
  )
}
