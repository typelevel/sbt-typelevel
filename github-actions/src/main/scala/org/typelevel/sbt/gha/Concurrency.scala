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

sealed abstract class Concurrency {
  def group: String
  def cancelInProgressExpr: Option[String]

  @deprecated("Use cancelInProgressExpr", "0.8.5")
  final def cancelInProgress: Option[Boolean] = cancelInProgressExpr match {
    case Some("false") => Some(false)
    case Some("true") => Some(true)
    case _ => None
  }
}

object Concurrency {
  def apply(group: String): Concurrency =
    Impl(group, None)

  def apply(group: String, cancelInProgress: Boolean): Concurrency =
    apply(group, Some(cancelInProgress))

  def apply(group: String, cancelInProgress: Option[Boolean]): Concurrency =
    Impl(group, cancelInProgress.map(_.toString))

  def apply(group: String, cancelInProgress: Option[String])(
      implicit dummy: DummyImplicit): Concurrency =
    Impl(group, cancelInProgress)

  private final case class Impl(group: String, cancelInProgress: Option[String])
      extends Concurrency {
    override def productPrefix = "Concurrency"
  }
}
