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

sealed abstract class WorkflowJob extends Product with Serializable {
  def id: String
  def name: String
  def needs: List[String]
  def outputs: Map[String, String]
  def permissions: Option[Permissions]
  def concurrency: Option[Concurrency]
  // TODO: Check for other common properites, like `cond` and `need`

  def withId(id: String): WorkflowJob
  def withName(name: String): WorkflowJob
  def withNeeds(needs: List[String]): WorkflowJob
  def withOutputs(outputs: Map[String, String]): WorkflowJob
  def withPermissions(permissions: Option[Permissions]): WorkflowJob
  def withConcurrency(concurrency: Option[Concurrency]): WorkflowJob

  def updatedOutputs(name: String, value: String): WorkflowJob
  def concatOutputs(outputs: TraversableOnce[(String, String)]): WorkflowJob
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
      outputs: Map[String, String] = Map.empty,
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
      timeoutMinutes: Option[Int] = None
  ): Run =
    Run(
      id = id,
      name = name,
      steps = steps,
      sbtStepPreamble = sbtStepPreamble,
      cond = cond,
      permissions = permissions,
      env = env,
      outputs = outputs,
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
  sealed abstract class Run extends WorkflowJob {
    def id: String
    def name: String
    def steps: List[WorkflowStep]
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

    def withId(id: String): Run
    def withName(name: String): Run
    def withSteps(steps: List[WorkflowStep]): Run
    def withSbtStepPreamble(sbtStepPreamble: List[String]): Run
    def withCond(cond: Option[String]): Run
    def withPermissions(permissions: Option[Permissions]): Run
    def withEnv(env: Map[String, String]): Run
    def withOutputs(outputs: Map[String, String]): Run
    def withOses(oses: List[String]): Run
    def withScalas(scalas: List[String]): Run
    def withJavas(javas: List[JavaSpec]): Run
    def withNeeds(needs: List[String]): Run
    def withMatrixFailFast(matrixFailFast: Option[Boolean]): Run
    def withMatrixAdds(matrixAdds: Map[String, List[String]]): Run
    def withMatrixIncs(matrixIncs: List[MatrixInclude]): Run
    def withMatrixExcs(matrixExcs: List[MatrixExclude]): Run
    def withRunsOnExtraLabels(runsOnExtraLabels: List[String]): Run
    def withContainer(container: Option[JobContainer]): Run
    def withEnvironment(environment: Option[JobEnvironment]): Run
    def withConcurrency(concurrency: Option[Concurrency]): Run
    def withTimeoutMinutes(timeoutMinutes: Option[Int]): Run

    def updatedEnv(name: String, value: String): Run
    def concatEnv(envs: TraversableOnce[(String, String)]): Run
    def updatedOutputs(name: String, value: String): Run
    def concatOutputs(outputs: TraversableOnce[(String, String)]): Run
    def appendedStep(step: WorkflowStep): Run
    def concatSteps(suffixSteps: TraversableOnce[WorkflowStep]): Run
  }
  object Run {
    def apply(
        id: String,
        name: String,
        steps: List[WorkflowStep],
        sbtStepPreamble: List[String] = List(s"++ $${{ matrix.scala }}"),
        cond: Option[String] = None,
        permissions: Option[Permissions] = None,
        env: Map[String, String] = Map(),
        outputs: Map[String, String] = Map.empty,
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
        timeoutMinutes: Option[Int] = None
    ): Run =
      Impl(
        id = id,
        name = name,
        steps = steps,
        sbtStepPreamble = sbtStepPreamble,
        cond = cond,
        permissions = permissions,
        env = env,
        outputs = outputs,
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
        timeoutMinutes: Option[Int]
    ) extends Run {

      // scalafmt: { maxColumn = 200 }
      override def withId(id: String): Run = copy(id = id)
      override def withName(name: String): Run = copy(name = name)
      override def withSteps(steps: List[WorkflowStep]): Run = copy(steps = steps)
      override def withSbtStepPreamble(sbtStepPreamble: List[String]): Run = copy(sbtStepPreamble = sbtStepPreamble)
      override def withCond(cond: Option[String]): Run = copy(cond = cond)
      override def withPermissions(permissions: Option[Permissions]): Run = copy(permissions = permissions)
      override def withEnv(env: Map[String, String]): Run = copy(env = env)
      override def withOutputs(outputs: Map[String, String]): Run = copy(outputs = outputs)
      override def withOses(oses: List[String]): Run = copy(oses = oses)
      override def withScalas(scalas: List[String]): Run = copy(scalas = scalas)
      override def withJavas(javas: List[JavaSpec]): Run = copy(javas = javas)
      override def withNeeds(needs: List[String]): Run = copy(needs = needs)
      override def withMatrixFailFast(matrixFailFast: Option[Boolean]): Run = copy(matrixFailFast = matrixFailFast)
      override def withMatrixAdds(matrixAdds: Map[String, List[String]]): Run = copy(matrixAdds = matrixAdds)
      override def withMatrixIncs(matrixIncs: List[MatrixInclude]): Run = copy(matrixIncs = matrixIncs)
      override def withMatrixExcs(matrixExcs: List[MatrixExclude]): Run = copy(matrixExcs = matrixExcs)
      override def withRunsOnExtraLabels(runsOnExtraLabels: List[String]): Run = copy(runsOnExtraLabels = runsOnExtraLabels)
      override def withContainer(container: Option[JobContainer]): Run = copy(container = container)
      override def withEnvironment(environment: Option[JobEnvironment]): Run = copy(environment = environment)
      override def withConcurrency(concurrency: Option[Concurrency]): Run = copy(concurrency = concurrency)
      override def withTimeoutMinutes(timeoutMinutes: Option[Int]): Run = copy(timeoutMinutes = timeoutMinutes)

      override def updatedEnv(name: String, value: String): Run = copy(env = env.updated(name, value))
      override def concatEnv(envs: TraversableOnce[(String, String)]): Run = copy(env = this.env ++ envs)
      override def updatedOutputs(name: String, value: String): Run = copy(outputs = outputs.updated(name, value))
      override def concatOutputs(outputs: TraversableOnce[(String, String)]): Run = copy(outputs = this.outputs ++ outputs)
      override def appendedStep(step: WorkflowStep): Run = copy(steps = this.steps :+ step)
      override def concatSteps(suffixSteps: TraversableOnce[WorkflowStep]): Run = copy(steps = this.steps ++ suffixSteps)
      // scalafmt: { maxColumn = 96 }

      override def productPrefix = "WorkflowJob"
    }
  }

  sealed abstract class Use extends WorkflowJob {
    def id: String
    def name: String
    def uses: String
    def needs: List[String]
    def secrets: Option[Secrets]
    def params: Map[String, String]
    def outputs: Map[String, String]
    def permissions: Option[Permissions]
    def concurrency: Option[Concurrency]

    def withId(id: String): Use
    def withName(name: String): Use
    def withNeeds(needs: List[String]): Use
    def withUses(uses: String): Use
    def withSecrets(secrets: Option[Secrets]): Use
    def withParams(params: Map[String, String]): Use
    def withOutputs(outputs: Map[String, String]): Use
    def withPermissions(permissions: Option[Permissions]): Use
    def withConcurrency(concurrency: Option[Concurrency]): Use

    def updatedParams(name: String, value: String): Use
    def concatParams(params: TraversableOnce[(String, String)]): Use
    def updatedOutputs(name: String, value: String): Use
    def concatOutputs(outputs: TraversableOnce[(String, String)]): Use
  }
  object Use {
    def apply(
        id: String,
        name: String,
        uses: String,
        needs: List[String] = List.empty,
        secrets: Option[Secrets] = None,
        params: Map[String, String] = Map.empty,
        outputs: Map[String, String] = Map.empty,
        permissions: Option[Permissions] = None,
        concurrency: Option[Concurrency] = None
    ): Use = new Impl(
      id = id,
      name = name,
      uses = uses,
      needs = needs,
      secrets = secrets,
      params = params,
      outputs = outputs,
      permissions = permissions,
      concurrency = concurrency
    )
    private final case class Impl(
        id: String,
        name: String,
        uses: String,
        needs: List[String],
        secrets: Option[Secrets],
        params: Map[String, String],
        outputs: Map[String, String],
        permissions: Option[Permissions],
        concurrency: Option[Concurrency]
    ) extends Use {
      override def productPrefix = "Use"

      // scalafmt: { maxColumn = 200 }
      override def withId(id: String): Use = copy(id = id)
      override def withName(name: String): Use = copy(name = name)
      override def withNeeds(needs: List[String]): Use = copy(needs = needs)
      override def withUses(uses: String): Use = copy(uses = uses)
      override def withSecrets(secrets: Option[Secrets]): Use = copy(secrets = secrets)
      override def withParams(params: Map[String, String]): Use = copy(params = params)
      override def withOutputs(outputs: Map[String, String]): Use = copy(outputs = outputs)
      override def withPermissions(permissions: Option[Permissions]): Use = copy(permissions = permissions)
      override def withConcurrency(concurrency: Option[Concurrency]): Use = copy(concurrency = concurrency)
      // scalafmt: { maxColumn = 96 }

      override def updatedParams(name: String, value: String) =
        copy(params = params.updated(name, value))
      override def concatParams(params: TraversableOnce[(String, String)]) =
        copy(params = this.params ++ params)

      override def updatedOutputs(name: String, value: String): Use =
        copy(outputs = outputs.updated(name, value))
      override def concatOutputs(outputs: TraversableOnce[(String, String)]): Use =
        copy(outputs = this.outputs ++ outputs)
    }
  }
}
