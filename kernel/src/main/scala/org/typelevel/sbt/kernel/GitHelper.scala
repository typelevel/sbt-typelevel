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

package org.typelevel.sbt.kernel

import scala.util.Try

import scala.sys.process._

object GitHelper {

  /**
   * @param fromHead
   *   if `true`, only tags reachable from HEAD's history. If `false`, all tags in the repo.
   */
  def previousReleases(fromHead: Boolean = false): List[V] = {
    Try {
      val merged = if (fromHead) " --merged" else ""
      s"git tag --list$merged".!!.split("\n").toList.map(_.trim).collect {
        case V.Tag(version) => version
      }
    }.getOrElse(List.empty).sorted.reverse
  }

  def getTagOrHash(tags: Seq[String], hash: Option[String]): Option[String] =
    tags.collect { case v @ V.Tag(_) => v }.headOption.orElse(hash)

}
