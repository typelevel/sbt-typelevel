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

import java.net.URL

sealed abstract class JobEnvironment {
  def name: String
  def url: Option[URL]
}

object JobEnvironment {
  def apply(name: String, url: Option[URL] = None): JobEnvironment =
    Impl(name, url)

  private final case class Impl(name: String, url: Option[URL]) extends JobEnvironment {
    override def productPrefix = "JobEnvironment"
  }
}
