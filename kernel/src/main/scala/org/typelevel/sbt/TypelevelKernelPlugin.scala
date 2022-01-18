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
    addCommandAlias("tlReleaseLocal", mkCommand(List("reload", "project /", "+publishLocal")))

  private[sbt] def mkCommand(commands: List[String]): String = commands.mkString("; ", "; ", "")

}
