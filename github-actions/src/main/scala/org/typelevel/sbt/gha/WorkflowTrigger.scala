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
  sealed trait BranchesFilter extends Product with Serializable
  object BranchesFilter {
    final case class Branches(branches: List[String]) extends BranchesFilter
    final case class BranchesIgnore(branches: List[String]) extends BranchesFilter
  }

  sealed trait TagsFilter extends Product with Serializable
  object TagsFilter {
    final case class Tags(tags: List[String]) extends TagsFilter
    final case class TagsIgnore(tags: List[String]) extends TagsFilter
  }

  sealed trait PullRequest extends WorkflowTrigger {
    def paths: Paths
    def branchesFilter: Option[BranchesFilter]
    def types: List[PREventType]

    def withBranchesFilter(filter: Option[BranchesFilter]): PullRequest
    def withPaths(paths: Paths): PullRequest
    def withTypes(types: List[PREventType]): PullRequest
  }
  object PullRequest {
    def apply(
        branchesFilter: Option[BranchesFilter] = None,
        paths: Paths = Paths.None,
        types: List[PREventType] = List.empty
    ): PullRequest = Impl(
      branchesFilter = branchesFilter,
      paths = paths,
      types = types
    )

    private final case class Impl(
        branchesFilter: Option[BranchesFilter],
        paths: Paths,
        types: List[PREventType]
    ) extends PullRequest {
      override def productPrefix = "PullRequest"

      // scalafmt: { maxColumn = 200 }
      def withPaths(paths: Paths): PullRequest = copy(paths = paths)
      def withBranchesFilter(filter: Option[BranchesFilter]): PullRequest = copy(branchesFilter = filter)
      def withTypes(types: List[PREventType]): PullRequest = copy(types = types)
      // scalafmt: { maxColumn = 96 }
    }
  }

  sealed trait Push extends WorkflowTrigger {
    def branchesFilter: Option[BranchesFilter]
    def tagsFilter: Option[TagsFilter]
    def paths: Paths

    def withBranchesFilter(filter: Option[BranchesFilter]): Push
    def withTagsFilter(filter: Option[TagsFilter]): Push
    def withPaths(paths: Paths): Push
  }
  object Push {
    def apply(
        branchesFilter: Option[BranchesFilter] = None,
        tagsFilter: Option[TagsFilter] = None,
        paths: Paths = Paths.None
    ): Push = Impl(
      branchesFilter = branchesFilter,
      tagsFilter = tagsFilter,
      paths = paths
    )

    private final case class Impl(
        branchesFilter: Option[BranchesFilter],
        tagsFilter: Option[TagsFilter],
        paths: Paths
    ) extends Push {
      override def productPrefix = "Push"

      // scalafmt: { maxColumn = 200 }
      def withBranchesFilter(filter: Option[BranchesFilter]): Push = copy(branchesFilter = filter)
      def withTagsFilter(filter: Option[TagsFilter]): Push = copy(tagsFilter = filter)
      def withPaths(paths: Paths): Push = copy(paths = paths)
      // scalafmt: { maxColumn = 96 }
    }
  }

  sealed trait WorkflowCall extends WorkflowTrigger {
    def inputs: Map[String, WorkflowCallInput]

    def withInputs(value: Map[String, WorkflowCallInput]): WorkflowCall
    def updatedInputs(id: String, value: WorkflowCallInput): WorkflowCall
  }
  object WorkflowCall {
    def apply(inputs: (String, WorkflowCallInput)*): WorkflowTrigger =
      Impl(inputs = inputs.toMap)

    private final case class Impl(
        inputs: Map[String, WorkflowCallInput]
    ) extends WorkflowCall {
      override def productPrefix = "WorkflowCall"

      override def withInputs(value: Map[String, WorkflowCallInput]): WorkflowCall =
        copy(inputs = value)
      override def updatedInputs(id: String, value: WorkflowCallInput): WorkflowCall =
        copy(inputs = this.inputs.updated(id, value))
    }
  }

  sealed trait WorkflowDispatch extends WorkflowTrigger {
    def inputs: Map[String, WorkflowDispatchInput]

    def withInputs(value: Map[String, WorkflowDispatchInput]): WorkflowDispatch
    def updatedInputs(id: String, value: WorkflowDispatchInput): WorkflowDispatch
  }
  object WorkflowDispatch {
    def apply(inputs: (String, WorkflowDispatchInput)*): WorkflowTrigger =
      Impl(inputs = inputs.toMap)

    private final case class Impl(
        inputs: Map[String, WorkflowDispatchInput]
    ) extends WorkflowDispatch {
      override def productPrefix = "WorkflowDispatch"

      override def withInputs(value: Map[String, WorkflowDispatchInput]): WorkflowDispatch =
        copy(inputs = value)
      override def updatedInputs(id: String, value: WorkflowDispatchInput): WorkflowDispatch =
        copy(inputs = this.inputs.updated(id, value))
    }
  }

  sealed trait WorkflowDispatchInput {
    def `type`: WorkflowDispatchInputType
    def description: Option[String]
    def required: Boolean
    def default: Option[String]

    def withType(value: WorkflowDispatchInputType): WorkflowDispatchInput
    def withDescription(value: Option[String]): WorkflowDispatchInput
    def withRequired(value: Boolean): WorkflowDispatchInput
    def withDefault(value: Option[String]): WorkflowDispatchInput
  }
  object WorkflowDispatchInput {
    def apply(
        required: Boolean,
        `type`: WorkflowDispatchInputType
    ): WorkflowDispatchInput = Impl(
      `type` = `type`,
      required = required,
      default = None,
      description = None
    )
    private final case class Impl(
        `type`: WorkflowDispatchInputType,
        description: Option[String],
        required: Boolean,
        default: Option[String]
    ) extends WorkflowDispatchInput {
      override def productPrefix = "WorkflowDispatchInput"

      override def withType(value: WorkflowDispatchInputType): WorkflowDispatchInput =
        copy(`type` = value)
      override def withDescription(value: Option[String]): WorkflowDispatchInput =
        copy(description = value)
      override def withRequired(value: Boolean): WorkflowDispatchInput =
        copy(required = value)
      override def withDefault(value: Option[String]): WorkflowDispatchInput =
        copy(default = value)
    }
  }
  sealed trait WorkflowDispatchInputType extends Product with Serializable
  object WorkflowDispatchInputType {
    final case object Boolean extends WorkflowDispatchInputType
    final case object Number extends WorkflowDispatchInputType
    final case object Environment extends WorkflowDispatchInputType
    final case object String extends WorkflowDispatchInputType
    final case class Choice(options: List[String]) extends WorkflowDispatchInputType
  }

  sealed trait WorkflowCallInput {
    def `type`: WorkflowCallInputType
    def description: Option[String]
    def required: Boolean
    def default: Option[String]

    def withType(value: WorkflowCallInputType): WorkflowCallInput
    def withDescription(value: Option[String]): WorkflowCallInput
    def withRequired(value: Boolean): WorkflowCallInput
    def withDefault(value: Option[String]): WorkflowCallInput
  }
  object WorkflowCallInput {
    def apply(
        required: Boolean,
        `type`: WorkflowCallInputType
    ): WorkflowCallInput = Impl(
      `type` = `type`,
      required = required,
      default = None,
      description = None
    )
    private final case class Impl(
        `type`: WorkflowCallInputType,
        description: Option[String],
        required: Boolean,
        default: Option[String]
    ) extends WorkflowCallInput {
      override def productPrefix = "WorkflowCallInput"

      override def withType(value: WorkflowCallInputType) =
        copy(`type` = value)
      override def withDescription(value: Option[String]) =
        copy(description = value)
      override def withRequired(value: Boolean) =
        copy(required = value)
      override def withDefault(value: Option[String]) =
        copy(default = value)
    }
  }
  sealed trait WorkflowCallInputType extends Product with Serializable
  object WorkflowCallInputType {
    final case object Boolean extends WorkflowCallInputType
    final case object Number extends WorkflowCallInputType
    final case object String extends WorkflowCallInputType
  }

  // TODO: workflow_run
  sealed trait WorkflowRun {
    def workflows: List[String]
    def types: List[WorkflowRunType]
    def filter: WorkflowRun
  }

  sealed trait WorkflowRunType extends Product with Serializable
  object WorkflowRunTypes {
    case object Commpleted extends WorkflowRunType
    case object Requested extends WorkflowRunType
    case object InProgress extends WorkflowRunType
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

    private final case class Impl(toYaml: String) extends Raw {
      override def productPrefix = "Raw"
    }
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
   */
}
