package org.typelevel.sbt

import sbt._
import sbt.Keys._

import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.ReleaseKeys
import com.typesafe.tools.mima.plugin.{MimaPlugin, MimaKeys}
import net.virtualvoid.sbt.graph.{Plugin => GraphPlugin}

import Releasing.Stages

object TypelevelPlugin extends Plugin {

  object TypelevelKeys {

    lazy val series =
      SettingKey[ReleaseSeries]("series", "The current release series of this branch")

    lazy val stability =
      SettingKey[Stability]("stability", "The current stability of this branch")

    // used to compute `version`
    lazy val relativeVersion =
      SettingKey[Version.Relative]("relativeVersion", "The current version of this branch, relative to the current series")

    // can be left unset
    lazy val lastRelease =
      SettingKey[Version.Relative]("lastRelease", "The last release in the series of this branch")

    lazy val signArtifacts =
      SettingKey[Boolean]("signArtifacts", "Sign artifacts before publishing")

    lazy val knownDependencies =
      SettingKey[Seq[Dependency]]("knownDependencies", "List of dependencies known to satisfy binary compatibility")

    lazy val checkDependencies =
      TaskKey[Unit]("checkDependencies", "Check that there are no conflicting dependencies")

    lazy val githubDevs =
      SettingKey[Seq[Developer]]("githubDevs", "Developers of this project")

    // can be left unset
    lazy val githubProject =
      SettingKey[(String, String)]("githubProject", "User/organization and project name")

  }

  def typelevelDefaultSettings: Seq[Def.Setting[_]] =
    ReleasePlugin.releaseSettings ++
    MimaPlugin.mimaDefaultSettings ++
    GraphPlugin.graphSettings ++
    List(
      version in ThisBuild :=
        Version(TypelevelKeys.series.value, TypelevelKeys.relativeVersion.value).id,
      TypelevelKeys.stability := Stability.Development,

      TypelevelKeys.signArtifacts := true,
      ReleaseKeys.releaseProcess :=
        Stages.checks ++
        Stages.versions ++
        Stages.pre ++
        Stages.publish ++
        Stages.post,

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
      },

      TypelevelKeys.knownDependencies := Dependencies.known.all,
      TypelevelKeys.checkDependencies :=
        Dependencies.check(
          TypelevelKeys.knownDependencies.value.toList,
          streams.value.log,
          (GraphPlugin.moduleGraph in Compile).value.nodes
        ),

      TypelevelKeys.githubDevs := List(),
      pomExtra := pomExtra.value ++ {
        <developers>
          { TypelevelKeys.githubDevs.value.map(_.pomExtra) }
        </developers>
      } ++ {
        TypelevelKeys.githubProject.?.value match {
          case Some((org, project)) =>
            <scm>
              <connection>scm:git:github.com/{ org }/{ project }.git</connection>
              <developerConnection>scm:git:git@github.com:{ org }/{ project }.git</developerConnection>
              <url>https://github.com/{ org }/{ project }</url>
            </scm>
          case None =>
            Seq()
        }
      }

    )

}
