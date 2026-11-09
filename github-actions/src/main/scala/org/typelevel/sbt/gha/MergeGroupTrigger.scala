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

sealed abstract class MergeGroupTrigger {
  def types: List[MergeGroupEventType]

  def withTypes(types: List[MergeGroupEventType]): MergeGroupTrigger
}

object MergeGroupTrigger {
  def apply(types: List[MergeGroupEventType] = Nil): MergeGroupTrigger =
    Impl(types)

  private final case class Impl(types: List[MergeGroupEventType]) extends MergeGroupTrigger {
    override def withTypes(types: List[MergeGroupEventType]): MergeGroupTrigger =
      copy(types = types)

    override def productPrefix = "MergeGroupTrigger"
  }
}
