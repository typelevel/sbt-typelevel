package org.typelevel.sbt

import sbt._
import sbt.Keys._

import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.ReleaseKeys
import com.typesafe.tools.mima.plugin.{MimaPlugin, MimaKeys}
import net.virtualvoid.sbt.graph.{Plugin => GraphPlugin}

import Releasing.Stages

object TypelevelPlugin extends Plugin {

  type Sett = Def.Setting[_]

  def versioningSettings: Seq[Sett] = List(
    version in ThisBuild := Version(TypelevelKeys.series.value, TypelevelKeys.relativeVersion.value).id,
    TypelevelKeys.stability := Stability.Development
  )

  def releaseSettings: Seq[Sett] = ReleasePlugin.releaseSettings ++ List(
    TypelevelKeys.signArtifacts := true,
    ReleaseKeys.releaseProcess :=
      Stages.checks ++
      Stages.versions ++
      Stages.pre ++
      Stages.publish ++
      Stages.post
  )

  def mimaSettings: Seq[Sett] = MimaPlugin.mimaDefaultSettings ++ List(
    MimaKeys.previousArtifact := {
      TypelevelKeys.stability.value match {
        case Stability.Stable =>
          TypelevelKeys.lastRelease.?.value match {
            case Some(lR) =>
              val ver = Version(TypelevelKeys.series.value, lR)
              Some(organization.value % (name.value + "_" + scalaBinaryVersion.value) % ver.id)
            case None =>
              None
          }
        case Stability.Development =>
          None
      }
    }
  )

  def dependencySettings: Seq[Sett] = GraphPlugin.graphSettings ++ List(
    TypelevelKeys.knownDependencies := Dependencies.known.all,
    TypelevelKeys.checkDependencies :=
      Dependencies.check(TypelevelKeys.knownDependencies.value, streams.value.log, (GraphPlugin.moduleGraph in Compile).value.nodes)
  )

  def typelevelDefaultSettings: Seq[Sett] =
    versioningSettings ++
    releaseSettings ++
    mimaSettings ++
    dependencySettings

}
