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

sealed abstract class WorkflowTriggers {
  def push: Option[PushTrigger]
  def pullRequest: Option[PullRequestTrigger]
  def mergeGroup: Option[MergeGroupTrigger]

  def withPush(push: Option[PushTrigger]): WorkflowTriggers
  def withPullRequest(pullRequest: Option[PullRequestTrigger]): WorkflowTriggers
  def withMergeGroup(mergeGroup: Option[MergeGroupTrigger]): WorkflowTriggers
}

object WorkflowTriggers {
  def apply(
      push: Option[PushTrigger] = None,
      pullRequest: Option[PullRequestTrigger] = None,
      mergeGroup: Option[MergeGroupTrigger] = None): WorkflowTriggers =
    Impl(push, pullRequest, mergeGroup)

  private final case class Impl(
      push: Option[PushTrigger],
      pullRequest: Option[PullRequestTrigger],
      mergeGroup: Option[MergeGroupTrigger])
      extends WorkflowTriggers {

    override def withPush(push: Option[PushTrigger]): WorkflowTriggers = copy(push = push)
    override def withPullRequest(pullRequest: Option[PullRequestTrigger]): WorkflowTriggers =
      copy(pullRequest = pullRequest)
    override def withMergeGroup(mergeGroup: Option[MergeGroupTrigger]): WorkflowTriggers =
      copy(mergeGroup = mergeGroup)

    override def productPrefix = "WorkflowTriggers"
  }
}
