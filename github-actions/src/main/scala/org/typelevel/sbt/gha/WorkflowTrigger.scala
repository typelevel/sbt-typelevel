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

package org.typelevel.sbt.gha

sealed trait WorkflowTrigger
object WorkflowTrigger {
  // TODO: make bin compat friendly
  // TODO: Make branches and tags work like Paths
  case class PullRequest(
      branches: List[String] = List.empty,
      paths: Paths = Paths.None,
      branchesIgnore: List[String] = List.empty,
      tags: List[String] = List.empty,
      tagsIgnore: List[String] = List.empty,
      types: List[PREventType] = List.empty
  ) extends WorkflowTrigger

  case class Push(
      branches: List[String] = List.empty,
      branchesIgnore: List[String] = List.empty,
      paths: Paths = Paths.None,
      tags: List[String] = List.empty,
      tagsIgnore: List[String] = List.empty
  ) extends WorkflowTrigger
}
