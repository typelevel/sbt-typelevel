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

sealed abstract class PullRequestTrigger extends PushOrPullRequestTrigger {
  def types: List[PREventType]

  def withTypes(types: List[PREventType]): PullRequestTrigger
}

object PullRequestTrigger {
  def apply(
      branches: List[String] = Nil,
      branchesIgnore: List[String] = Nil,
      paths: List[String] = Nil,
      pathsIgnore: List[String] = Nil,
      types: List[PREventType] = Nil
  ): PullRequestTrigger =
    Impl(branches, branchesIgnore, paths, pathsIgnore, types)

  private final case class Impl(
      branches: List[String],
      branchesIgnore: List[String],
      paths: List[String],
      pathsIgnore: List[String],
      types: List[PREventType])
      extends PullRequestTrigger {

    override def withBranches(branches: List[String]): PullRequestTrigger =
      copy(branches = branches)
    override def withBranchesIgnore(branchesIgnore: List[String]): PullRequestTrigger =
      copy(branchesIgnore = branchesIgnore)
    override def withPaths(paths: List[String]): PullRequestTrigger = copy(paths = paths)
    override def withPathsIgnore(pathsIgnore: List[String]): PullRequestTrigger =
      copy(pathsIgnore = pathsIgnore)
    override def withTypes(types: List[PREventType]): PullRequestTrigger =
      copy(types = types)

    override def productPrefix = "PullRequestTrigger"
  }
}
