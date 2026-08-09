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

sealed abstract class PushTrigger extends PushOrPullRequestTrigger {
  def tags: List[String]
  def tagsIgnore: List[String]

  def withTags(tags: List[String]): PushTrigger
  def withTagsIgnore(tagsIgnore: List[String]): PushTrigger
}

object PushTrigger {
  def apply(
      branches: List[String] = Nil,
      branchesIgnore: List[String] = Nil,
      tags: List[String] = Nil,
      tagsIgnore: List[String] = Nil,
      paths: List[String] = Nil,
      pathsIgnore: List[String] = Nil): PushTrigger =
    Impl(branches, branchesIgnore, tags, tagsIgnore, paths, pathsIgnore)

  private final case class Impl(
      branches: List[String],
      branchesIgnore: List[String],
      tags: List[String],
      tagsIgnore: List[String],
      paths: List[String],
      pathsIgnore: List[String])
      extends PushTrigger {

    override def withBranches(branches: List[String]): PushTrigger = copy(branches = branches)
    override def withBranchesIgnore(branchesIgnore: List[String]): PushTrigger =
      copy(branchesIgnore = branchesIgnore)
    override def withTags(tags: List[String]): PushTrigger = copy(tags = tags)
    override def withTagsIgnore(tagsIgnore: List[String]): PushTrigger =
      copy(tagsIgnore = tagsIgnore)
    override def withPaths(paths: List[String]): PushTrigger = copy(paths = paths)
    override def withPathsIgnore(pathsIgnore: List[String]): PushTrigger =
      copy(pathsIgnore = pathsIgnore)

    override def productPrefix = "PushTrigger"
  }
}
