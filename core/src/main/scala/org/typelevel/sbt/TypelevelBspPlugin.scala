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

import sbt.Keys._
import sbt.VirtualAxis.PlatformAxis
import sbt._
import sbtprojectmatrix.ProjectMatrixKeys.virtualAxes
import sbtprojectmatrix.ProjectMatrixPlugin

object TypelevelBspPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires: Plugins = ProjectMatrixPlugin

  object autoImport {
    lazy val tlBspCrossProjectPlatforms: SettingKey[Set[PlatformAxis]] =
      settingKey[Set[PlatformAxis]](
        "A set of platforms for which BSP should be enabled (default: not initialized)")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[?]] = Seq(
    bspEnabled := {
      val oldValue = bspEnabled.value

      (virtualAxes.?.value, tlBspCrossProjectPlatforms.?.value) match {
        // This is a multi-platform project and `tlBspCrossProjectPlatforms` is set by a user explicitly.
        case (Some(axes), Some(bspPlatforms)) =>
          axes.exists {
            case projectPlatform: PlatformAxis => bspPlatforms.contains(projectPlatform)
            case _ => false
          }
        case _ => oldValue
      }
    }
  )
}
