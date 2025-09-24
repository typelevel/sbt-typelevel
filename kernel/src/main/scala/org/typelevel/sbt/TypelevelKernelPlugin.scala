/*
 * Copyright 2022 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.sbt

import org.typelevel.sbt.kernel.GitHelper
import org.typelevel.sbt.kernel.V
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import scala.annotation.nowarn

object TypelevelKernelPlugin extends AutoPlugin {

  override def requires = JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    @deprecated("use `Provided` instead", "0.6.1")
    lazy val CompileTime: Configuration = config("compile-time").hide

    lazy val tlIsScala3 = settingKey[Boolean]("True if building with Scala 3")

    @deprecated(
      "No longer has an effect. Use tlCrossRootProject for your root project.",
      "0.5.0")
    lazy val tlSkipIrrelevantScalas = settingKey[Boolean](
      "Sets skip := true for compile/test/publish/etc. tasks on a project if the current scalaVersion is not in that project's crossScalaVersions (default: false)")

    lazy val tlCommandAliases = settingKey[Map[String, List[String]]](
      "Command aliases defined for this build"
    )

    lazy val tlPrePRSteps =
      settingKey[List[String]]("Steps to be performed before a user submits a PR")

    @deprecated(
      "Use `tlCommandAliases` for a more composable command definition experience",
      "0.6.1")
    def tlReplaceCommandAlias(name: String, contents: String): Seq[Setting[State => State]] =
      Seq(GlobalScope / onLoad ~= { (f: State => State) =>
        f andThen { s: State =>
          BasicCommands.addAlias(BasicCommands.removeAlias(s, name), name, contents)
        }
      })

  }

  import autoImport._

  override def globalSettings = Seq(
    Def.derive(tlIsScala3 := scalaVersion.value.startsWith("3.")),
    tlCommandAliases := Map(
      "tlReleaseLocal" -> List("reload", "project /", "+publishLocal"),
      "prePR" -> ("reload" :: "project /" :: tlPrePRSteps.value)
    ),
    onLoad := {
      val aliases = tlCommandAliases.value
      onLoad.value.compose { (state: State) =>
        aliases.foldLeft(state) {
          case (state, (alias, command)) =>
            BasicCommands.addAlias(state, alias, mkCommand(command))
        }
      }
    },
    onUnload := {
      val aliases = tlCommandAliases.value.keys
      onUnload.value.compose { (state: State) =>
        aliases.foldLeft(state) { (state, alias) => BasicCommands.removeAlias(state, alias) }
      }
    },
    tlPrePRSteps := List.empty
  )

  @nowarn("cat=deprecation")
  override def projectSettings = Seq(
    ivyConfigurations += CompileTime,
    Compile / unmanagedClasspath ++= update.value.select(configurationFilter(CompileTime.name))
  )

  private[sbt] def mkCommand(commands: List[String]): String = commands.mkString("; ", "; ", "")

  private[sbt] lazy val currentRelease: Def.Initialize[Option[String]] = Def.setting {
    // some tricky logic here ...
    // if the latest release is a pre-release (e.g., M or RC)
    // and there are no stable releases it is bincompatible with,
    // then for all effective purposes it is the current release

    val release = previousReleases.value match {
      case head :: tail if head.isPrerelease =>
        tail
          .filterNot(_.isPrerelease)
          .find(head.copy(prerelease = None).mustBeBinCompatWith(_))
          .orElse(Some(head))
      case releases => releases.headOption
    }

    release.map(_.toString)
  }

  // latest tagged release, including pre-releases
  private[sbt] lazy val currentPreRelease: Def.Initialize[Option[String]] = Def.setting {
    previousReleases.value.headOption.map(_.toString)
  }

  private[this] lazy val previousReleases: Def.Initialize[List[V]] = Def.setting {
    val currentVersion = V(version.value).map(_.copy(prerelease = None))
    GitHelper.previousReleases(fromHead = true, strict = false).filter { v =>
      currentVersion.forall(v.copy(prerelease = None) <= _)
    }
  }

}
