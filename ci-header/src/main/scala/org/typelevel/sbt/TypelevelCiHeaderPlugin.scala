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
import org.typelevel.sbt.gha.GenerativePlugin
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin

object TypelevelCiHeaderPlugin extends AutoPlugin {

  import GenerativePlugin.autoImport._

  override def requires = GenerativePlugin
  override def trigger = allRequirements

  override def buildSettings = Seq(
    githubWorkflowBuild ~= { steps =>
      WorkflowStep.Sbt(List("headerCheckAll"), name = Some("Check headers")) +: steps
    }
  )

  override def projectSettings = AutomateHeaderPlugin.projectSettings

}
