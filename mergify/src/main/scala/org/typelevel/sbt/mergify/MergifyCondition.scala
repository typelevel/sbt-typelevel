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

sealed abstract class MergifyCondition

object MergifyCondition {
  implicit def encoder: Encoder[MergifyCondition] = Encoder.instance {
    case custom: Custom => custom.asJson
    case and: And => and.asJson
    case or: Or => or.asJson
    case _ => sys.error("shouldn't happen")
  }

  final case class Custom(condition: String) extends MergifyCondition
  object Custom {
    implicit def encoder: Encoder[Custom] = Encoder.encodeString.contramap(_.condition)
  }

  final case class And(conditions: List[MergifyCondition]) extends MergifyCondition
  object And {
    implicit def encoder: Encoder[And] = Encoder.forProduct1("and")(_.conditions)
  }

  final case class Or(conditions: List[MergifyCondition]) extends MergifyCondition
  object Or {
    implicit def encoder: Encoder[Or] = Encoder.forProduct1("or")(_.conditions)
  }

  @nowarn("cat=unused")
  private[this] final object Dummy extends MergifyCondition // break exhaustivity checking
}
