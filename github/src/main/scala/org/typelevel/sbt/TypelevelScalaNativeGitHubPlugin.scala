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

import com.github.sbt.git.SbtGit.git
import scala.scalanative.sbtplugin.ScalaNativePlugin
import org.typelevel.sbt.kernel.GitHelper
import sbt._

import Keys._

object TypelevelScalaNativeGitHubPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = ScalaNativePlugin && TypelevelKernelPlugin

  import TypelevelKernelPlugin.autoImport._

  override def projectSettings = Seq(
    scalacOptions ++= {
      val flag = "-P:scalanative:mapSourceURI:"

      val tagOrHash =
        GitHelper.getTagOrHash(git.gitCurrentTags.value, git.gitHeadCommit.value)

      val l = (LocalRootProject / baseDirectory).value.toURI.toString

      tagOrHash.flatMap { v =>
        scmInfo.value.map { info =>
          val g =
            s"${info.browseUrl.toString.replace("github.com", "raw.githubusercontent.com")}/$v/"
          s"$flag$l->$g"
        }
      }
    }
  )
}
