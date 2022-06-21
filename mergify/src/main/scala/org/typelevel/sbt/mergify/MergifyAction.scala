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

import io.circe.Encoder
import io.circe.syntax._

import scala.annotation.nowarn

sealed abstract class MergifyAction {
  private[mergify] def name = getClass.getSimpleName.toLowerCase
}

object MergifyAction {

  implicit def encoder: Encoder[MergifyAction] = Encoder.instance {
    case merge: Merge => merge.asJson
    case label: Label => label.asJson
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

  @nowarn("cat=unused")
  private[this] object Dummy extends MergifyAction
}
