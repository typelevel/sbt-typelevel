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
import sbt._
import sbtcrossproject.CrossPlugin
import sbtcrossproject.CrossPlugin.autoImport._
import sbtcrossproject.Platform

object TypelevelBspPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires: Plugins = CrossPlugin

  object autoImport {
    lazy val tlBspCrossProjectPlatforms: SettingKey[Set[Platform]] =
      settingKey[Set[Platform]](
        "set of platforms for which BSP should be enabled (default: not initialized)")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[?]] = Seq(
    bspEnabled := {
      val oldValue = bspEnabled.value

      // We have to check if `crossProjectCrossType` and `crossProjectPlatform`
      // are initialized. Otherwise, the `sbt-typelevel` won't load itself.
      (
        crossProjectCrossType.?.value,
        crossProjectPlatform.?.value,
        tlBspCrossProjectPlatforms.?.value
      ) match {
        // `tlBspCrossProjectPlatforms` is set by a user explicitly.
        case (_, Some(projectPlatform), Some(bspPlatforms)) =>
          bspPlatforms.contains(projectPlatform)
        // If `CrossType` is `Pure` then enable BSP for the JVM platform only.
        case (Some(CrossType.Pure), Some(projectPlatform), None) =>
          projectPlatform == JVMPlatform
        // Otherwise simply return the old value.
        case _ => oldValue
      }
    }
  )
}
