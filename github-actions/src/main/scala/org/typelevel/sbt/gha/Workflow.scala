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

sealed abstract class Workflow {
  def name: Option[String]
  def runName: Option[String]
  def on: List[WorkflowTrigger]
  def permissions: Option[Permissions]
  def env: Map[String, String]
  def concurrency: Option[Concurrency]
  def jobs: List[WorkflowJob]

  // scalafmt: { maxColumn = 200 }
  def withName(name: Option[String]): Workflow
  def withRunName(runName: Option[String]): Workflow
  def withOn(on: List[WorkflowTrigger]): Workflow
  def withPermissions(permissions: Option[Permissions]): Workflow
  def withEnv(env: Map[String, String]): Workflow
  def withConcurrency(concurrency: Option[Concurrency]): Workflow
  def withJobs(jobs: List[WorkflowJob]): Workflow

  def appendedOn(on: WorkflowTrigger): Workflow
  def concatOns(suffixOn: TraversableOnce[WorkflowTrigger]): Workflow

  def updatedEnv(name: String, value: String): Workflow
  def concatEnv(envs: TraversableOnce[(String, String)]): Workflow

  def appendedJob(job: WorkflowJob): Workflow
  def concatJobs(suffixJobs: TraversableOnce[WorkflowJob]): Workflow
  // scalafmt: { maxColumn = 96 }
}

object Workflow {
  def apply(on: List[WorkflowTrigger]): Workflow = Impl(
    name = Option.empty,
    runName = Option.empty,
    jobs = List.empty,
    on = on,
    permissions = Option.empty,
    env = Map.empty,
    concurrency = Option.empty
  )

  private final case class Impl(
      name: Option[String],
      runName: Option[String],
      on: List[WorkflowTrigger],
      permissions: Option[Permissions],
      env: Map[String, String],
      concurrency: Option[Concurrency],
      jobs: List[WorkflowJob]
  ) extends Workflow {

    // scalafmt: { maxColumn = 200 }
    override def withName(name: Option[String]): Workflow = copy(name = name)
    override def withRunName(runName: Option[String]): Workflow = copy(runName = runName)
    override def withOn(on: List[WorkflowTrigger]): Workflow = copy(on = on)
    override def withPermissions(permissions: Option[Permissions]): Workflow = copy(permissions = permissions)
    override def withEnv(env: Map[String, String]): Workflow = copy(env = env)
    override def withConcurrency(concurrency: Option[Concurrency]): Workflow = copy(concurrency = concurrency)
    override def withJobs(jobs: List[WorkflowJob]): Workflow = copy(jobs = jobs)

    def appendedOn(on: WorkflowTrigger): Workflow = copy(on = this.on :+ on)
    def concatOns(suffixOn: TraversableOnce[WorkflowTrigger]): Workflow = copy(on = this.on ++ on)

    def updatedEnv(name: String, value: String): Workflow = copy(env = this.env.updated(name, value))
    def concatEnv(envs: TraversableOnce[(String, String)]): Workflow = copy(env = this.env ++ envs)

    override def appendedJob(job: WorkflowJob): Workflow = copy(jobs = this.jobs :+ job)
    override def concatJobs(suffixJobs: TraversableOnce[WorkflowJob]): Workflow = copy(jobs = this.jobs ++ jobs)
    // scalafmt: { maxColumn = 96 }

    override def productPrefix = "Workflow"
  }
}
