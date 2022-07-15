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

import sbt.*
import sbt.plugins.JvmPlugin
import Keys.*
import org.typelevel.sbt.kernel.{GitHelper, V}

object TypelevelKernelPlugin extends AutoPlugin {

  override def requires = JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    lazy val tlIsScala3 = settingKey[Boolean]("True if building with Scala 3")
    lazy val tlSkipIrrelevantScalas = settingKey[Boolean](
      "Sets skip := true for compile/test/publish/etc. tasks on a project if the current scalaVersion is not in that project's crossScalaVersions (default: false)")

    def tlReplaceCommandAlias(name: String, contents: String): Seq[Setting[State => State]] =
      Seq(GlobalScope / onLoad ~= { (f: State => State) =>
        f andThen { s: State =>
          BasicCommands.addAlias(BasicCommands.removeAlias(s, name), name, contents)
        }
      })

    private[sbt] lazy val currentReleaseImpl = Def.setting {
      // some tricky logic here ...
      // if the latest release is a pre-release (e.g., M or RC)
      // and there are no stable releases it is bincompatible with,
      // then for all effective purposes it is the current release

      val release = previousReleasesImpl.value match {
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
    private[sbt] lazy val currentPreReleaseImpl = Def.setting {
      previousReleasesImpl.value.headOption.map(_.toString)
    }

    private[sbt] lazy val previousReleasesImpl = Def.setting {
      val currentVersion = V(version.value).map(_.copy(prerelease = None))
      GitHelper.previousReleases(fromHead = true, strict = false).filter { v =>
        currentVersion.forall(v.copy(prerelease = None) <= _)
      }
    }
  }

  import autoImport._

  override def globalSettings = Seq(
    Def.derive(tlIsScala3 := scalaVersion.value.startsWith("3."))
  )

  override def buildSettings =
    Seq(tlSkipIrrelevantScalas := false) ++
      addCommandAlias("tlReleaseLocal", mkCommand(List("reload", "project /", "+publishLocal")))

  override def projectSettings = Seq(
    (Test / test) := {
      if (tlSkipIrrelevantScalas.value && (Test / test / skip).value)
        ()
      else (Test / test).value
    },
    (Compile / doc) := {
      if (tlSkipIrrelevantScalas.value && (Compile / doc / skip).value)
        (Compile / doc / target).value
      else (Compile / doc).value
    },
    skipIfIrrelevant(compile),
    skipIfIrrelevant(test),
    skipIfIrrelevant(doc),
    skipIfIrrelevant(publishLocal),
    skipIfIrrelevant(publish)
  )

  private[sbt] def mkCommand(commands: List[String]): String = commands.mkString("; ", "; ", "")

  /**
   * A setting that will make a task respect the `tlSkipIrrelevantScalas` setting. Note that the
   * task itself must respect `skip` for this to take effect.
   */
  def skipIfIrrelevant[T](task: TaskKey[T]) = {
    task / skip := {
      (task / skip).value || {
        val cross = crossScalaVersions.value
        val ver = (LocalRootProject / scalaVersion).value
        (task / tlSkipIrrelevantScalas).value && !cross.contains(ver)
      }
    }
  }

}
