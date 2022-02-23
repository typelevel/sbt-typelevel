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

sealed abstract class MergifyAction {
  def name: String
}

object MergifyAction {

  final case class Merge(
      method: Option[String] = None,
      rebaseFallback: Option[String] = None,
      commitMessageTemplate: Option[String] = None
  ) extends MergifyAction {
    def name = "merge"
  }

  final case class Label(
      add: List[String] = Nil,
      remove: List[String] = Nil,
      removeAll: Option[Boolean] = None
  ) extends MergifyAction {
    def name = "label"
  }

  private[this] object Dummy extends MergifyAction { // break exhaustivity checking
    def name = "dummy"
  }

}
