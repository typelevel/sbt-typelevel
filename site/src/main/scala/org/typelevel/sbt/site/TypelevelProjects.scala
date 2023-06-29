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

package org.typelevel.sbt.site

import sbt._

sealed abstract class TypelevelProject
object TypelevelProject {
  private[sbt] case object Organization extends TypelevelProject
  case object Affiliate extends TypelevelProject

  val Cats = tl("cats")
  val CatsEffect = tl("cats-effect")
  val Discipline = gh("discipline")
  val Fs2 = "fs2" -> url("https://fs2.io/")
  val Http4s = "http4s" -> url("https://http4s.org/")
  val Scalacheck = "scalacheck" -> url("https://scalacheck.org/")
  val Shapeless = gh("shapeless", "milessabin")
  val Shapeless3 = gh("shapeless-3")

  private def tl(repo: String) = repo -> url(s"https://typelevel.org/$repo/")
  private def gh(repo: String, user: String = "typelevel") =
    repo -> url(s"https://github.com/$user/$repo/")
}
