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

package org.typelevel.sbt.mergify

import cats.data._
import cats.syntax.all._
import io.circe._
import io.circe.syntax._
import org.typelevel.sbt.mergify.MergifyAction.RequestReviews._
import sbt.librarymanagement.Developer

sealed abstract class MergifyAction {
  private[mergify] def name = getClass.getSimpleName.toLowerCase
}

object MergifyAction {

  implicit def encoder: Encoder[MergifyAction] = Encoder.instance {
    case merge: Merge => merge.asJson
    case label: Label => label.asJson
    case requestReviews: RequestReviews => requestReviews.asJson
    case Update => Update.asJson
    case _ => sys.error("should not happen")
  }

  final case class Merge(
      method: Option[String] = None,
      rebaseFallback: Option[String] = None,
      commitMessageTemplate: Option[String] = None
  ) extends MergifyAction

  object Merge {
    implicit def encoder: Encoder[Merge] =
      Encoder.forProduct3("method", "rebase_fallback", "commit_message_template") {
        (m: Merge) => (m.method, m.rebaseFallback, m.commitMessageTemplate)
      }
  }

  final case class Label(
      add: List[String] = Nil,
      remove: List[String] = Nil,
      removeAll: Option[Boolean] = None
  ) extends MergifyAction

  object Label {
    implicit def encoder: Encoder[Label] =
      Encoder.forProduct3("add", "remove", "remove_all") { (l: Label) =>
        (l.add, l.remove, l.removeAll)
      }
  }

  class RequestReviews private (
      private val users: Option[OptionallyWeighted],
      private val teams: Option[OptionallyWeighted],
      private val usersFromTeams: Option[OptionallyWeighted],
      private val randomCount: Option[Int])
      extends MergifyAction {
    override private[mergify] def name = "request_reviews"

    private def copy(
        users: Option[OptionallyWeighted] = users,
        teams: Option[OptionallyWeighted] = teams,
        usersFromTeams: Option[OptionallyWeighted] = usersFromTeams,
        randomCount: Option[Int] = randomCount): RequestReviews =
      new RequestReviews(users, teams, usersFromTeams, randomCount) {}

    def andUsers(user: String, users: String*): RequestReviews =
      copy(users = Unweighted(NonEmptyList.of(user, users: _*)).some)

    def andUsers(user: (String, Int), users: (String, Int)*): RequestReviews =
      copy(users = Weighted(NonEmptyList.of(user, users: _*)).some)

    def andTeams(team: String, teams: String*): RequestReviews =
      copy(teams = Unweighted(NonEmptyList.of(team, teams: _*)).some)

    def andTeams(team: (String, Int), teams: (String, Int)*): RequestReviews =
      copy(teams = Weighted(NonEmptyList.of(team, teams: _*)).some)

    def andUsersFromTeams(team: String, teams: String*): RequestReviews =
      copy(usersFromTeams = Unweighted(NonEmptyList.of(team, teams: _*)).some)

    def andUsersFromTeams(team: (String, Int), teams: (String, Int)*): RequestReviews =
      copy(usersFromTeams = Weighted(NonEmptyList.of(team, teams: _*)).some)

    def withRandomCount(count: Int): RequestReviews =
      copy(randomCount = Option(count))

    def andDevelopers(developers: List[Developer]): RequestReviews =
      copy(users = NonEmptyList.fromList(developers.map(_.id)).map(Unweighted))
  }

  object RequestReviews {
    def fromUsers(user: String, users: String*) =
      new RequestReviews(Unweighted(NonEmptyList.of(user, users: _*)).some, None, None, None)
    def fromUsers(user: (String, Int), users: (String, Int)*) =
      new RequestReviews(Weighted(NonEmptyList.of(user, users: _*)).some, None, None, None)
    def fromTeams(team: String, teams: String*) =
      new RequestReviews(None, Unweighted(NonEmptyList.of(team, teams: _*)).some, None, None)
    def fromTeams(team: (String, Int), teams: (String, Int)*) =
      new RequestReviews(None, Weighted(NonEmptyList.of(team, teams: _*)).some, None, None)
    def fromUsersOfTeams(team: String, teams: String*) =
      new RequestReviews(None, None, Unweighted(NonEmptyList.of(team, teams: _*)).some, None)
    def fromUsersOfTeams(team: (String, Int), teams: (String, Int)*) =
      new RequestReviews(None, None, Weighted(NonEmptyList.of(team, teams: _*)).some, None)

    def apply(developers: List[Developer]) =
      new RequestReviews(
        Unweighted(
          developers
            .map(_.id)
            .toNel
            .getOrElse(throw new RuntimeException("developers must be non-empty"))
        ).some,
        None,
        None,
        None)

    implicit def encoder: Encoder[RequestReviews] =
      Encoder.forProduct4("users", "teams", "users_from_teams", "random_count") {
        requestReviews =>
          (
            requestReviews.users,
            requestReviews.teams,
            requestReviews.usersFromTeams,
            requestReviews.randomCount
          )
      }

    private sealed trait OptionallyWeighted
    private case class Weighted(value: NonEmptyList[(String, Int)]) extends OptionallyWeighted
    private case class Unweighted(value: NonEmptyList[String]) extends OptionallyWeighted

    private object OptionallyWeighted {
      implicit val encoder: Encoder[OptionallyWeighted] = {
        case Weighted(value) => value.asJson
        case Unweighted(value) => value.asJson
      }
    }
  }

  object Update extends MergifyAction {
    override private[mergify] def name = "update"

    implicit def encoder: Encoder[Update.type] =
      Encoder[JsonObject].contramap(_ => JsonObject.empty)
  }

  private[this] object Dummy extends MergifyAction

}
