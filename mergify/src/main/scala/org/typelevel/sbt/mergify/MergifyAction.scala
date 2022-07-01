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

  final class RequestReviews(
      val users: Either[NonEmptyList[String], NonEmptyMap[String, Int]],
      val randomCount: Option[Int])
      extends MergifyAction {
    override private[mergify] def name = "request_reviews"
  }

  object RequestReviews {
    def apply(user: String, users: String*) =
      new RequestReviews(NonEmptyList.of(user, users: _*).asLeft, None)

    def apply(weightedUser: (String, Int), weightedUsers: (String, Int)*) =
      new RequestReviews(NonEmptyMap.of(weightedUser, weightedUsers: _*).asRight, None)

    def apply(randomCount: Int, user: String, users: String*) =
      new RequestReviews(NonEmptyList.of(user, users: _*).asLeft, Option(randomCount))

    def apply(randomCount: Int, weightedUser: (String, Int), weightedUsers: (String, Int)*) =
      new RequestReviews(
        NonEmptyMap.of(weightedUser, weightedUsers: _*).asRight,
        Option(randomCount)
      )

    implicit def encoder: Encoder[RequestReviews] =
      Encoder.forProduct2("users", "random_count") { requestReviews =>
        (requestReviews.users.fold(_.asJson, _.asJson), requestReviews.randomCount)
      }
  }

  object Update extends MergifyAction {
    override private[mergify] def name = "update"

    implicit def encoder: Encoder[Update.type] =
      Encoder[JsonObject].contramap(_ => JsonObject.empty)
  }

  private[this] object Dummy extends MergifyAction

}
