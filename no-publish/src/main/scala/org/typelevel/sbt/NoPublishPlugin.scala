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

object NoPublishPlugin extends AutoPlugin {
  object autoImport {
    lazy val noPublishProjectRefs = settingKey[Seq[ProjectRef]]("List of no-publish projects")
  }
  import autoImport._

  private lazy val noPublishInternalAggregation =
    settingKey[Seq[ProjectRef]]("Aggregates all the no-publish projects")

  override def trigger = noTrigger

  override def globalSettings = Seq(
    noPublishInternalAggregation := Seq(),
    noPublishProjectRefs := noPublishInternalAggregation.value
  )

  override def projectSettings = Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publish / skip := true,
    Global / noPublishInternalAggregation += thisProjectRef.value
  )
}
