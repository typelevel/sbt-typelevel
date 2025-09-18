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
  def uses: Option[String]
  def secrets: Secrets
  def sbtStepPreamble: List[String]
  def cond: Option[String]
  def permissions: Option[Permissions]
  def env: Map[String, String]
  def outputs: Map[String, String]
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

  def withId(id: String): WorkflowJob
  def withName(name: String): WorkflowJob
  def withSteps(steps: List[WorkflowStep]): WorkflowJob
  def withUses(uses: Option[String]): WorkflowJob
  def withSecrets(secrets: Secrets): WorkflowJob
  def withSbtStepPreamble(sbtStepPreamble: List[String]): WorkflowJob
  def withCond(cond: Option[String]): WorkflowJob
  def withPermissions(permissions: Option[Permissions]): WorkflowJob
  def withEnv(env: Map[String, String]): WorkflowJob
  def withOutputs(env: Map[String, String]): WorkflowJob
  def withOses(oses: List[String]): WorkflowJob
  def withScalas(scalas: List[String]): WorkflowJob
  def withJavas(javas: List[JavaSpec]): WorkflowJob
  def withNeeds(needs: List[String]): WorkflowJob
  def withMatrixFailFast(matrixFailFast: Option[Boolean]): WorkflowJob
  def withMatrixAdds(matrixAdds: Map[String, List[String]]): WorkflowJob
  def withMatrixIncs(matrixIncs: List[MatrixInclude]): WorkflowJob
  def withMatrixExcs(matrixExcs: List[MatrixExclude]): WorkflowJob
  def withRunsOnExtraLabels(runsOnExtraLabels: List[String]): WorkflowJob
  def withContainer(container: Option[JobContainer]): WorkflowJob
  def withEnvironment(environment: Option[JobEnvironment]): WorkflowJob
  def withConcurrency(concurrency: Option[Concurrency]): WorkflowJob
  def withTimeoutMinutes(timeoutMinutes: Option[Int]): WorkflowJob

  def updatedEnv(name: String, value: String): WorkflowJob
  def concatEnv(envs: TraversableOnce[(String, String)]): WorkflowJob
  def updatedOutputs(name: String, value: String): WorkflowJob
  def concatOutputs(outputs: TraversableOnce[(String, String)]): WorkflowJob
  def appendedStep(step: WorkflowStep): WorkflowJob
  def concatSteps(suffixSteps: TraversableOnce[WorkflowStep]): WorkflowJob
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
      oses: List[String] = List("ubuntu-22.04"),
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
      id = id,
      name = name,
      steps = steps,
      uses = None,
      secrets = Secrets.empty,
      sbtStepPreamble = sbtStepPreamble,
      cond = cond,
      permissions = permissions,
      env = env,
      outputs = Map.empty,
      oses = oses,
      scalas = scalas,
      javas = javas,
      needs = needs,
      matrixFailFast = matrixFailFast,
      matrixAdds = matrixAdds,
      matrixIncs = matrixIncs,
      matrixExcs = matrixExcs,
      runsOnExtraLabels = runsOnExtraLabels,
      container = container,
      environment = environment,
      concurrency = concurrency,
      timeoutMinutes = timeoutMinutes
    )

  private final case class Impl(
      id: String,
      name: String,
      steps: List[WorkflowStep],
      uses: Option[String],
      secrets: Secrets,
      sbtStepPreamble: List[String],
      cond: Option[String],
      permissions: Option[Permissions],
      env: Map[String, String],
      outputs: Map[String, String],
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
      timeoutMinutes: Option[Int])
      extends WorkflowJob {

    // scalafmt: { maxColumn = 200 }
    override def withId(id: String): WorkflowJob = copy(id = id)
    override def withName(name: String): WorkflowJob = copy(name = name)
    override def withSteps(steps: List[WorkflowStep]): WorkflowJob = copy(steps = steps)
    override def withUses(uses: Option[String]): WorkflowJob = copy(uses = uses)
    override def withSecrets(secrets: Secrets): WorkflowJob = copy(secrets = secrets)
    override def withSbtStepPreamble(sbtStepPreamble: List[String]): WorkflowJob = copy(sbtStepPreamble = sbtStepPreamble)
    override def withCond(cond: Option[String]): WorkflowJob = copy(cond = cond)
    override def withPermissions(permissions: Option[Permissions]): WorkflowJob = copy(permissions = permissions)
    override def withEnv(env: Map[String, String]): WorkflowJob = copy(env = env)
    override def withOutputs(outputs: Map[String, String]): WorkflowJob = copy(outputs = outputs)
    override def withOses(oses: List[String]): WorkflowJob = copy(oses = oses)
    override def withScalas(scalas: List[String]): WorkflowJob = copy(scalas = scalas)
    override def withJavas(javas: List[JavaSpec]): WorkflowJob = copy(javas = javas)
    override def withNeeds(needs: List[String]): WorkflowJob = copy(needs = needs)
    override def withMatrixFailFast(matrixFailFast: Option[Boolean]): WorkflowJob = copy(matrixFailFast = matrixFailFast)
    override def withMatrixAdds(matrixAdds: Map[String, List[String]]): WorkflowJob = copy(matrixAdds = matrixAdds)
    override def withMatrixIncs(matrixIncs: List[MatrixInclude]): WorkflowJob = copy(matrixIncs = matrixIncs)
    override def withMatrixExcs(matrixExcs: List[MatrixExclude]): WorkflowJob = copy(matrixExcs = matrixExcs)
    override def withRunsOnExtraLabels(runsOnExtraLabels: List[String]): WorkflowJob = copy(runsOnExtraLabels = runsOnExtraLabels)
    override def withContainer(container: Option[JobContainer]): WorkflowJob = copy(container = container)
    override def withEnvironment(environment: Option[JobEnvironment]): WorkflowJob = copy(environment = environment)
    override def withConcurrency(concurrency: Option[Concurrency]): WorkflowJob = copy(concurrency = concurrency)
    override def withTimeoutMinutes(timeoutMinutes: Option[Int]): WorkflowJob = copy(timeoutMinutes = timeoutMinutes)

    def updatedEnv(name: String, value: String): WorkflowJob = copy(env = env.updated(name, value))
    def concatEnv(envs: TraversableOnce[(String, String)]): WorkflowJob = copy(env = this.env ++ envs)
    def updatedOutputs(name: String, value: String): WorkflowJob = copy(outputs = outputs.updated(name, value))
    def concatOutputs(outputs: TraversableOnce[(String, String)]): WorkflowJob = copy(outputs = this.outputs ++ outputs)
    def appendedStep(step: WorkflowStep): WorkflowJob = copy(steps = this.steps :+ step)
    def concatSteps(suffixSteps: TraversableOnce[WorkflowStep]): WorkflowJob = copy(steps = this.steps ++ suffixSteps)
    // scalafmt: { maxColumn = 96 }

    override def productPrefix = "WorkflowJob"
  }
}
