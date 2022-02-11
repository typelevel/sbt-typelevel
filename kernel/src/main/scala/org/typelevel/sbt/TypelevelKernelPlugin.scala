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

import sbt._, Keys._
import sbt.plugins.JvmPlugin

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
  }

  import autoImport._

  override def globalSettings = Seq(
    Def.derive(tlIsScala3 := scalaVersion.value.startsWith("3."))
  )

  override def buildSettings =
    Seq(tlSkipIrrelevantScalas := true) ++
      addCommandAlias("tlReleaseLocal", mkCommand(List("reload", "project /", "+publishLocal")))

  override def projectSettings = Seq(
    (Test / test) := {
      if ((Test / test / skip).value)
        ()
      else (Test / test).value
    },
    skipIfIrrelevant(compile),
    skipIfIrrelevant(test),
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
