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

sealed abstract class JobContainer {
  def image: String
  def credentials: Option[(String, String)]
  def env: Map[String, String]
  def volumes: Map[String, String]
  def ports: List[Int]
  def options: List[String]
}

object JobContainer {

  def apply(
      image: String,
      credentials: Option[(String, String)] = None,
      env: Map[String, String] = Map(),
      volumes: Map[String, String] = Map(),
      ports: List[Int] = Nil,
      options: List[String] = Nil): JobContainer =
    Impl(image, credentials, env, volumes, ports, options)

  private[gha] def unapply(jc: JobContainer) = {
    import jc._
    Some((image, credentials, env, volumes, ports, options))
  }

  private final case class Impl(
      image: String,
      credentials: Option[(String, String)],
      env: Map[String, String],
      volumes: Map[String, String],
      ports: List[Int],
      options: List[String])
      extends JobContainer {
    override def productPrefix: String = "JobContainer"
  }
}
