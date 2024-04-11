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

sealed abstract class WorkflowJob {
  def id: String
  def name: String
  def steps: List[WorkflowStep]
  def sbtStepPreamble: List[String]
  def cond: Option[String]
  def permissions: Option[Permissions]
  def env: Map[String, String]
  def oses: List[String]
  def scalas: List[String]
  def javas: List[JavaSpec]
  def needs: List[String]
  def matrixFailFast: Option[Boolean]
  def matrixAdds: Map[String, List[String]]
  def matrixIncs: List[MatrixInclude]
  def matrixExcs: List[MatrixExclude]
  def runsOnExtraLabels: List[String]
  def container: Option[JobContainer]
  def environment: Option[JobEnvironment]
  def concurrency: Option[Concurrency]
  def timeoutMinutes: Option[Int]
}

object WorkflowJob {
  def apply(
      id: String,
      name: String,
      steps: List[WorkflowStep],
      sbtStepPreamble: List[String] = List(s"++ $${{ matrix.scala }}"),
      cond: Option[String] = None,
      permissions: Option[Permissions] = None,
      env: Map[String, String] = Map(),
      oses: List[String] = List("ubuntu-latest"),
      scalas: List[String] = List("2.13"),
      javas: List[JavaSpec] = List(JavaSpec.temurin("11")),
      needs: List[String] = List(),
      matrixFailFast: Option[Boolean] = None,
      matrixAdds: Map[String, List[String]] = Map(),
      matrixIncs: List[MatrixInclude] = List(),
      matrixExcs: List[MatrixExclude] = List(),
      runsOnExtraLabels: List[String] = List(),
      container: Option[JobContainer] = None,
      environment: Option[JobEnvironment] = None,
      concurrency: Option[Concurrency] = None,
      timeoutMinutes: Option[Int] = None): WorkflowJob =
    Impl(
      id,
      name,
      steps,
      sbtStepPreamble,
      cond,
      permissions,
      env,
      oses,
      scalas,
      javas,
      needs,
      matrixFailFast,
      matrixAdds,
      matrixIncs,
      matrixExcs,
      runsOnExtraLabels,
      container,
      environment,
      concurrency,
      timeoutMinutes
    )

  private final case class Impl(
      id: String,
      name: String,
      steps: List[WorkflowStep],
      sbtStepPreamble: List[String],
      cond: Option[String],
      permissions: Option[Permissions],
      env: Map[String, String],
      oses: List[String],
      scalas: List[String],
      javas: List[JavaSpec],
      needs: List[String],
      matrixFailFast: Option[Boolean],
      matrixAdds: Map[String, List[String]],
      matrixIncs: List[MatrixInclude],
      matrixExcs: List[MatrixExclude],
      runsOnExtraLabels: List[String],
      container: Option[JobContainer],
      environment: Option[JobEnvironment],
      concurrency: Option[Concurrency],
      timeoutMinutes: Option[Int]) extends WorkflowJob {
    override def productPrefix = "Impl"
  }
}
