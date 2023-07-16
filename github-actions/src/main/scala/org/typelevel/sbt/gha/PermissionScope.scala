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

sealed abstract class Permissions extends Product with Serializable

/**
 * @see
 *   https://docs.github.com/en/actions/using-jobs/assigning-permissions-to-jobs#overview
 */
object Permissions {
  case object ReadAll extends Permissions
  case object WriteAll extends Permissions
  case object None extends Permissions
  final case class Specify private (
      actions: PermissionValue,
      checks: PermissionValue,
      contents: PermissionValue,
      deployments: PermissionValue,
      idToken: PermissionValue,
      issues: PermissionValue,
      packages: PermissionValue,
      pages: PermissionValue,
      pullRequests: PermissionValue,
      repositoryProjects: PermissionValue,
      securityEvents: PermissionValue,
      statuses: PermissionValue
  ) extends Permissions {
    private[gha] lazy val asMap: Map[PermissionScope, PermissionValue] = Map(
      PermissionScope.Actions -> actions,
      PermissionScope.Checks -> checks,
      PermissionScope.Contents -> contents,
      PermissionScope.Deployments -> deployments,
      PermissionScope.IdToken -> idToken,
      PermissionScope.Issues -> issues,
      PermissionScope.Packages -> packages,
      PermissionScope.Pages -> pages,
      PermissionScope.PullRequests -> pullRequests,
      PermissionScope.RepositoryProjects -> repositoryProjects,
      PermissionScope.SecurityEvents -> securityEvents,
      PermissionScope.Statuses -> statuses
    )
  }
  object Specify {
    // See https://docs.github.com/en/actions/security-guides/automatic-token-authentication#permissions-for-the-github_token
    val defaultPermissive = Specify(
      actions = PermissionValue.Write,
      checks = PermissionValue.Write,
      contents = PermissionValue.Write,
      deployments = PermissionValue.Write,
      idToken = PermissionValue.None,
      issues = PermissionValue.Write,
      packages = PermissionValue.Write,
      pages = PermissionValue.Write,
      pullRequests = PermissionValue.Write,
      repositoryProjects = PermissionValue.Write,
      securityEvents = PermissionValue.Write,
      statuses = PermissionValue.Write
    )

    val defaultRestrictive = Specify(
      actions = PermissionValue.None,
      checks = PermissionValue.None,
      contents = PermissionValue.Read,
      deployments = PermissionValue.None,
      idToken = PermissionValue.None,
      issues = PermissionValue.None,
      packages = PermissionValue.Read,
      pages = PermissionValue.None,
      pullRequests = PermissionValue.None,
      repositoryProjects = PermissionValue.None,
      securityEvents = PermissionValue.None,
      statuses = PermissionValue.None
    )

    val maxPRAccessFromFork = Specify(
      actions = PermissionValue.Read,
      checks = PermissionValue.Read,
      contents = PermissionValue.Read,
      deployments = PermissionValue.Read,
      idToken = PermissionValue.Read,
      issues = PermissionValue.Read,
      packages = PermissionValue.Read,
      pages = PermissionValue.Read,
      pullRequests = PermissionValue.Read,
      repositoryProjects = PermissionValue.Read,
      securityEvents = PermissionValue.Read,
      statuses = PermissionValue.Read
    )
  }
}

sealed abstract class PermissionScope extends Product with Serializable

object PermissionScope {
  case object Actions extends PermissionScope
  case object Checks extends PermissionScope
  case object Contents extends PermissionScope
  case object Deployments extends PermissionScope
  case object IdToken extends PermissionScope
  case object Issues extends PermissionScope
  case object Discussions extends PermissionScope
  case object Packages extends PermissionScope
  case object Pages extends PermissionScope
  case object PullRequests extends PermissionScope
  case object RepositoryProjects extends PermissionScope
  case object SecurityEvents extends PermissionScope
  case object Statuses extends PermissionScope
}

sealed abstract class PermissionValue extends Product with Serializable

object PermissionValue {
  case object Read extends PermissionValue
  case object Write extends PermissionValue
  case object None extends PermissionValue
}
