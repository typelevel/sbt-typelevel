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
  sealed trait PullRequest extends WorkflowTrigger {
    def branches: List[String]
    def paths: Paths
    def branchesIgnore: List[String]
    def tags: List[String]
    def tagsIgnore: List[String]
    def types: List[PREventType]

    def withBranches(branches: List[String]): PullRequest
    def withPaths(paths: Paths): PullRequest
    def withBranchesIgnore(branchesIgnore: List[String]): PullRequest
    def withTags(tags: List[String]): PullRequest
    def withTagsIgnore(tagsIgnore: List[String]): PullRequest
    def withTypes(types: List[PREventType]): PullRequest
  }
  object PullRequest {
    def apply(
        branches: List[String] = List.empty,
        paths: Paths = Paths.None,
        branchesIgnore: List[String] = List.empty,
        tags: List[String] = List.empty,
        tagsIgnore: List[String] = List.empty,
        types: List[PREventType] = List.empty
    ): PullRequest = Impl(
      branches = branches,
      paths = paths,
      branchesIgnore = branchesIgnore,
      tags = tags,
      tagsIgnore = tagsIgnore,
      types = types
    )

    private final case class Impl(
        branches: List[String],
        paths: Paths,
        branchesIgnore: List[String],
        tags: List[String],
        tagsIgnore: List[String],
        types: List[PREventType]
    ) extends PullRequest {
      override def productPrefix = "PullRequest"

      // scalafmt: { maxColumn = 200 }
      def withBranches(branches: List[String]): PullRequest = copy(branches = branches)
      def withPaths(paths: Paths): PullRequest = copy(paths = paths)
      def withBranchesIgnore(branchesIgnore: List[String]): PullRequest = copy(branchesIgnore = branchesIgnore)
      def withTags(tags: List[String]): PullRequest = copy(tags = tags)
      def withTagsIgnore(tagsIgnore: List[String]): PullRequest = copy(tagsIgnore = tagsIgnore)
      def withTypes(types: List[PREventType]): PullRequest = copy(types = types)
      // scalafmt: { maxColumn = 96 }
    }
  }

  sealed trait Push extends WorkflowTrigger {
    def branches: List[String]
    def branchesIgnore: List[String]
    def paths: Paths
    def tags: List[String]
    def tagsIgnore: List[String]

    def withBranches(branches: List[String]): Push
    def withBranchesIgnore(branchesIgnore: List[String]): Push
    def withPaths(paths: Paths): Push
    def withTags(tags: List[String]): Push
    def withTagsIgnore(tagsIgnore: List[String]): Push
  }
  object Push {
    def apply(
        branches: List[String] = List.empty,
        branchesIgnore: List[String] = List.empty,
        paths: Paths = Paths.None,
        tags: List[String] = List.empty,
        tagsIgnore: List[String] = List.empty
    ): Push = Impl(
      branches = branches,
      branchesIgnore = branchesIgnore,
      paths = paths,
      tags = tags,
      tagsIgnore = tagsIgnore
    )

    private final case class Impl(
        branches: List[String],
        branchesIgnore: List[String],
        paths: Paths,
        tags: List[String],
        tagsIgnore: List[String]
    ) extends Push {
      override def productPrefix = "Push"

      // scalafmt: { maxColumn = 200 }
      def withBranches(branches: List[String]): Push = copy(branches = branches)
      def withBranchesIgnore(branchesIgnore: List[String]): Push = copy(branchesIgnore = branchesIgnore)
      def withPaths(paths: Paths): Push = copy(paths = paths)
      def withTags(tags: List[String]): Push = copy(tags = tags)
      def withTagsIgnore(tagsIgnore: List[String]): Push = copy(tagsIgnore = tagsIgnore)
      // scalafmt: { maxColumn = 96 }
    }
  }

  sealed trait WorkflowCall extends WorkflowTrigger
  object WorkflowCall {
    def apply(): WorkflowCall = Impl
    private final case object Impl extends WorkflowCall {
      override def productPrefix = "WorkflowCall"
    }
  }

  /**
   * A workflow trigger, inserted directly into the yaml, with 1 level of indention. This is an
   * escape hatch for people wanting triggers other than the supported ones.
   */
  sealed trait Raw extends WorkflowTrigger {
    def toYaml: String
  }
  object Raw {
    def raw(yaml: String): Raw = Impl(yaml)

    private final case class Impl(toYaml: String) extends Raw
  }

  /*
   * Other Triggers not implemented here:
   *
   * branch_protection_rule
   * check_run
   * check_suite
   * create
   * delete
   * deployment
   * deployment_status
   * discussion
   * discussion_comment
   * fork
   * gollum
   * issue_comment
   * issues
   * label
   * merge_group
   * milestone
   * page_build
   * public
   * pull_request_comment (use issue_comment)
   * pull_request_review
   * pull_request_review_comment
   * pull_request_target
   * registry_package
   * release
   * repository_dispatch
   * schedule
   * status
   * watch
   * workflow_disbranch_protection_rule
   * check_run
   * check_suite
   * create
   * delete
   * deployment
   * deployment_status
   * discussion
   * discussion_comment
   * fork
   * gollum
   * issue_comment
   * issues
   * label
   * merge_group
   * milestone
   * page_build
   * public
   * pull_request
   * pull_request_comment (use issue_comment)
   * pull_request_review
   * pull_request_review_comment
   * pull_request_target
   * push
   * registry_package
   * release
   * repository_dispatch
   * schedule
   * status
   * watch
   * workflow_dispatch
   * workflow_runpatch
   * workflow_run
   */
}
