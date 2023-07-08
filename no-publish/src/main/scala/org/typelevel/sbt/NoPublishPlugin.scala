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

import sbt._

import Keys._
import NoPublishGlobalPlugin.autoImport.noPublishModulesIgnore

object NoPublishPlugin extends AutoPlugin {

  override def trigger = noTrigger

  override def projectSettings = Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publish / skip := true,
    Global / noPublishModulesIgnore ++= crossScalaVersions.value.flatMap { v =>
      // the binary versions are needed for the modules-ignore in Submit Dependencies
      // it's best to pick them up here instead of guessing in the CI plugin
      CrossVersion(crossVersion.value, v, CrossVersion.binaryScalaVersion(v)).map(cross => cross(thisProjectRef.value.project))
    }
  )
}

object NoPublishGlobalPlugin extends AutoPlugin {

  // triggered even if NoPublishPlugin is not used in the build
  override def trigger = allRequirements

  object autoImport {
    private[sbt] lazy val noPublishModulesIgnore =
      settingKey[Seq[String]]("List of no-publish projects and their scala cross-versions")
  }

  import autoImport._

  override def globalSettings = Seq(
    noPublishModulesIgnore := Seq()
  )

}
