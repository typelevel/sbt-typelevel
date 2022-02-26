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

final case class MergifyStewardConfig(
    name: String = "merge scala-steward's PRs",
    author: String = "scala-steward",
    mergeMinors: Boolean = false,
    action: MergifyAction.Merge = MergifyAction.Merge()
) {

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
