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

sealed abstract class MergifyStewardConfig {

  def name: String
  def author: String
  def mergeMinors: Boolean
  def action: MergifyAction.Merge

  def withName(name: String): MergifyStewardConfig
  def withAuthor(author: String): MergifyStewardConfig
  def withMergeMinors(mergeMinors: Boolean): MergifyStewardConfig
  def withAction(action: MergifyAction.Merge): MergifyStewardConfig

  private[mergify] def toPrRule(buildConditions: List[MergifyCondition]): MergifyPrRule = {
    val authorCond = MergifyCondition.Custom(s"author=$author")

    val bodyCond = {
      val patchCond = MergifyCondition.Custom("body~=labels:.*early-semver-patch")
      val minorCond = MergifyCondition.Custom("body~=labels:.*early-semver-minor")
      if (mergeMinors) MergifyCondition.Or(List(patchCond, minorCond))
      else patchCond
    }

    MergifyPrRule(
      name,
      authorCond :: bodyCond :: buildConditions,
      List(action)
    )
  }

}

object MergifyStewardConfig {
  def apply(
      name: String = "merge scala-steward's PRs",
      author: String = "scala-steward",
      mergeMinors: Boolean = false,
      action: MergifyAction.Merge = MergifyAction.Merge()
  ): MergifyStewardConfig = Impl(name, author, mergeMinors, action)

  private final case class Impl(
      name: String,
      author: String,
      mergeMinors: Boolean,
      action: MergifyAction.Merge
  ) extends MergifyStewardConfig {
    override def productPrefix = "MergifyStewardConfig"

    def withName(name: String) = copy(name = name)
    def withAuthor(author: String) = copy(author = author)
    def withMergeMinors(mergeMinors: Boolean) = copy(mergeMinors = mergeMinors)
    def withAction(action: MergifyAction.Merge) = copy(action = action)
  }
}
